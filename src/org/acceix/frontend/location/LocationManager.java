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

package org.acceix.frontend.location;


import org.acceix.frontend.crud.CoreModule;
import org.acceix.frontend.crud.models.CrudObject;
import org.acceix.frontend.crud.ObjectOperations;
import org.acceix.frontend.crud.loaders.ObjectLoader;
import org.acceix.frontend.crud.models.CrudTable;
import org.acceix.frontend.helpers.ActionSettings;
import org.acceix.frontend.helpers.ModuleHelper;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.parser.ParseException;


public class LocationManager extends org.acceix.frontend.helpers.ModuleHelper {
    
    private CrudObject crudObject;
    private CrudTable crudTable;   
    private ObjectOperations crudOperations;    


    @Override
    public void construct() {
        setModuleName("location");
        addAction(new ActionSettings("showlocation", true, this::showlocation));

    }
    
        public ModuleHelper getInstance() {
            return new LocationManager();
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
    
    
     public void showlocation() {

        
                if (setupAndChecks()==false) return;
                

                if (!isRoleAviableForUser(crudObject.getRoleRead())) {
                    
                        addToDataModel("result", "error");
                        addToDataModel("message", "Access denied !");
                        try {
                            renderData(crudObject.getTemplateForLocation());
                        } catch (IOException ex) {
                            Logger.getLogger(CoreModule.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        return;
                        
                }
                

                
                crudObject.setUser_id(getUserId());
                crudObject.setDomain(getDomain());

                try {

                    Map<String,Object> data = crudOperations.getLocationFromDatabase(crudObject,
                                                                                    crudTable,
                                                                                    crudTable.getIdFieldName(),
                                                                                    Integer.valueOf(getParameterOrDefault("row_id","-1")),
                                                                                    getRequestObject());
                                                            
                    
                    data.keySet().forEach(dataKey -> {
                            addToDataModel(dataKey,data.get(dataKey));
                    });
                    


                } catch (ClassNotFoundException | SQLException | IOException | ParseException ex) {
                    addToDataModel("result", "error");
                    addToDataModel("message", "Access denied !");
                        try {
                            renderData(crudObject.getTemplateForLocation());
                        } catch (IOException ex1) {
                            Logger.getLogger(CoreModule.class.getName()).log(Level.SEVERE, null, ex1);
                        }                    
                    Logger.getLogger(CoreModule.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
                
              

                crudObject.getMetaDataKeys().forEach((metadata_key) -> {
                    addToDataModel(metadata_key, crudObject.getMetaData(metadata_key));
                });

                
        try {
            renderData(crudObject.getTemplateForLocation());
        } catch (IOException ex) {
            Logger.getLogger(CoreModule.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }  
     

     
    
    
}
