/*
 * The MIT License
 *
 * Copyright 2022 Rza Asadov (rza dot asadov at gmail dot com).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.acceix.frontend.files;

import org.acceix.frontend.crud.CoreModule;
import org.acceix.frontend.crud.models.CrudObject;
import org.acceix.frontend.crud.ObjectOperations;
import org.acceix.frontend.crud.loaders.ObjectLoader;
import org.acceix.frontend.crud.models.CrudTable;
import org.acceix.frontend.helpers.ActionSettings;
import org.acceix.frontend.helpers.ModuleHelper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import javax.xml.bind.DatatypeConverter;
import org.acceix.ndatabaseclient.DataConnector;
import org.acceix.ndatabaseclient.MachineDataSet;
import org.eclipse.jetty.util.IO;
import org.json.simple.parser.ParseException;


public class UploadManager extends org.acceix.frontend.helpers.ModuleHelper {
    
    private CrudObject crudObject;
    private CrudTable crudTable;   
    private ObjectOperations crudOperations;
    

    @Override
    public void construct() {
        setModuleName("files");
        addAction(new ActionSettings("upload", false, this::upload));
        addAction(new ActionSettings("showfiles", false, this::showfiles));
    }
    
        public ModuleHelper getInstance() {
            return new UploadManager();
        }    
    
       public boolean setupAndChecks() {
        
                crudOperations = new ObjectOperations(this);
        
                String obj = (String) getParameter("obj");
                String table = (String) getParameter("table");
                
                ObjectLoader loader = new ObjectLoader();
                
                if (!loader.isExist(obj)) {
                    addToDataModel("message", "Wrong object request !");
                    addToDataModel("result", "error");
                    renderData();
                    return false;
                }
                
                crudObject = loader.get(obj);
                
                if (crudObject != null) {
                
                        if ( crudObject.getCrudTables().keySet().size() >= 1 && table != null) {
                                crudTable = crudObject.getCrudTable(table);
                        } else if ( crudObject.getCrudTables().keySet().size() >= 1 && table == null ) {
                                crudTable = crudObject.getDefaultCrudTable();
                        } else if ( crudObject.getCrudTables().keySet().isEmpty() && table != null ) {
                                addToDataModel("message", "Unable find such table");
                                addToDataModel("result", "error");
                                renderData();
                                return false;
                        } else {
                                //addToDataModel("message", "Wrong system call !");
                                //addToDataModel("result", "error");
                                //renderData();
                                //return false;
                        }

                        if  (crudObject.isRequireAuth() && !isUserAuthenticatedBySession() && !isAuthenticatedByToken()) {
                            addToDataModel("message", "Something goes wrong with request authentication!");
                            addToDataModel("authByToken",isAuthenticatedByToken());
                            addToDataModel("authBySession",isUserAuthenticatedBySession());
                            addToDataModel("objectRequireAuth",crudObject.isRequireAuth());
                            addToDataModel("result", "error");
                            renderData();
                            return false;
                        }

                        addToDataModel("cur_obj",crudObject.getName());
                        return true;
                        
                } else {
                        addToDataModel("message", "Wrong object name !");
                        addToDataModel("result", "error");
                        renderData();
                        return false;
                }
    } 
    
    public void upload () {
        
        try {
            for (Part part : getHttpServletRequest().getParts())
            {
                //System.out.printf("Got Part[%s].size=%s%n", part.getName(), part.getSize());
                //System.out.printf("Got Part[%s].contentType=%s%n", part.getName(), part.getContentType());
                //System.out.printf("Got Part[%s].submittedFileName=%s%n", part.getName(), part.getSubmittedFileName());
                String orig_filename = part.getSubmittedFileName();
                String filename;
                String fileExtension = null;
                if (!orig_filename.isEmpty()) {
                    // ensure we don't have "/" and ".." in the raw form.
                    filename = URLEncoder.encode(orig_filename, "utf-8");
                    
                    
                    if (filename.contains(".")) {
                        try {
                            String[] filenameSplitted = filename.split("\\.");
                            
                            if (filenameSplitted.length > 2) {
                                addToDataModel("result", "error");
                                addToDataModel("message", "Filename incorrect");
                                renderData();
                                return;
                            }

                            fileExtension = filenameSplitted[1].toLowerCase();
                            
                            filename = getUserId() + "_" + System.currentTimeMillis() + "_" + generateFilename(filename) + "." + fileExtension;
                        } catch (NoSuchAlgorithmException ex) {
                            Logger.getLogger(UploadManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                            addToDataModel("result", "error");
                            addToDataModel("message", "Incorrect file extension format");                            
                            renderData();
                            return;                      
                    }
                    
                    
                    
                    String outputFile = getGlobalEnvs().get("upload_dir") + "/" + filename;

                        try {
                            MachineDataSet dataSet =  new DataConnector(getGlobalEnvs(),getUsername()).getTable("npt_file_types")
                                    .select()
                                        .getColumn("id")
                                    .where()
                                        .eq("extension", fileExtension)
                                    .compile()
                                    .executeSelect();
                            if (dataSet.next()) {
                                
                                try (InputStream inputStream = part.getInputStream();
                                        OutputStream outputStream = Files.newOutputStream(Paths.get(outputFile),
                                                                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
                                {
                                    IO.copy(inputStream, outputStream); 
                                }                                
                                
                                
                                int file_id = new DataConnector(getGlobalEnvs(),getUsername()).getTable("npt_files")
                                                                    .insert()
                                                                        .add("user_id", getUserId())
                                                                        .add("type", dataSet.getInteger("id"))
                                                                        .add("file_path", filename)
                                                                        .add("status", 1)
                                                                        .add("file_size", part.getSize())
                                                                        .add("orig_name", orig_filename)
                                                                    .compile()
                                                                    .executeAndGetID();
                                
                                addToDataModel("result", "success");
                                addToDataModel("message", "Upload Complete");
                                addToDataModel("file_id", file_id);
                                renderData();                                
                                return;
                            } else {
                                addToDataModel("result", "error");
                                addToDataModel("message", "File extension not allowed");
                                renderData();
                                return;                                
                            }
                        } catch (ClassNotFoundException | SQLException ex) {
                            
                            Logger.getLogger(UploadManager.class.getName()).log(Level.SEVERE, null, ex);
                                addToDataModel("result", "error");
                                addToDataModel("message", "File extension not allowed");
                                renderData();
                                return;                              
                        }
                                                            

                }
            }
        } catch (IOException | ServletException ex) {
            Logger.getLogger(UploadManager.class.getName()).log(Level.SEVERE, null, ex);
        } 

    }
    
    public void showfiles() {

                // Fix it !!!!!!!!!!!!!!!!!!!!!!!
                if (setupAndChecks()==false) return;
                

                if (!isRoleAviableForUser(crudObject.getRoleRead())) {
                    
                        addToDataModel("result", "error");
                        addToDataModel("message", "Access denied !");
                        try {
                            renderData(crudObject.getTemplateForFiles());
                        } catch (IOException ex) {
                            Logger.getLogger(CoreModule.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        return;
                        
                }
                
                var headers = new LinkedList<>();
                
                headers.add("Filename");
                headers.add("File size");
                headers.add("Actions");
                addToDataModel("headers", headers);
                
                crudObject.setUser_id(getUserId());
                crudObject.setDomain(getDomain());
                
                try {

                    addToDataModel("data", crudOperations.getFileListFromDatabase(crudObject,
                                                                                    crudTable,
                                                                                    crudTable.getIdFieldName(),
                                                                                    Integer.valueOf(getParameterOrDefault("row_id","-1")),
                                                                                    getRequestObject()
                                                                                 )                
                                  );

                } catch (ClassNotFoundException | SQLException | IOException | ParseException ex) {
                    Logger.getLogger(CoreModule.class.getName()).log(Level.SEVERE, null, ex);
                }
                

                addToDataModel("containstable", true);
                addToDataModel("creatable", crudObject.isCreatable());
                addToDataModel("editable", crudObject.isEditable());

                crudObject.getMetaDataKeys().forEach((metadata_key) -> {
                    addToDataModel(metadata_key, crudObject.getMetaData(metadata_key));
                });

                
        try {
            renderData(crudObject.getTemplateForFiles());
        } catch (IOException ex) {
            Logger.getLogger(CoreModule.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }    
    
    
    private String generateFilename(String password) throws NoSuchAlgorithmException {
        
        MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            byte[] digest = md.digest();
            return DatatypeConverter
              .printHexBinary(digest).toUpperCase();        
            }

}