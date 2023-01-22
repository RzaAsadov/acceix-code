/*
 * The MIT License
 *
 * Copyright 2022 Rza Asadov (rza at asadov dot me).
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
import org.acceix.frontend.crud.ObjectReadOperations;
import org.acceix.frontend.crud.loaders.ObjectLoader;
import org.acceix.frontend.crud.models.CrudTable;
import org.acceix.frontend.helpers.ActionSettings;
import org.acceix.frontend.helpers.ModuleHelper;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.acceix.frontend.crud.ObjectCommonOperations;
import org.json.simple.parser.ParseException;


public class LocationManager extends org.acceix.frontend.helpers.ModuleHelper {
    
    private CrudObject crudObject;
    private CrudTable crudTable;   
    private ObjectCommonOperations crudCommonOperations;    
    private ObjectReadOperations crudReadOperations;    


    @Override
    public void construct() {
        setModuleName("location");
        addAction(new ActionSettings("showlocation", true, this::showlocation));
        addAction(new ActionSettings("updatelocation", true, this::updatelocation));

    }
    
        public ModuleHelper getInstance() {
            return new LocationManager();
        }    
    
    public boolean setupAndChecks() {
        
                crudCommonOperations = new ObjectCommonOperations(this);
                crudReadOperations = new ObjectReadOperations(this);
        
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

                    Map<String,Object> data = crudReadOperations.getLocationFromDatabase(crudObject,
                                                                                    crudTable,
                                                                                    crudTable.getIdFieldName(),
                                                                                    Integer.valueOf(getParameterOrDefault("row_id","-1")),
                                                                                    getRequestObject());
                                                            
                    
                    data.keySet().forEach(dataKey -> {
                            addToDataModel(dataKey,data.get(dataKey));
                    });
                    
                    addToDataModel("doafter","loadContainerQuery('crud','read','#netondocontentbody','obj=" + crudObject.getName() + "');");                

                    addToDataModel("row_id",getParameterOrDefault("row_id","-1"));
                    addToDataModel("submit_to_module", "location");
                    addToDataModel("submit_to_action", "updatelocation");                    


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
     
    
     public void updatelocation() {

        
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
                
                var inputParams = getRequestObject().getParams();

                var t_inputParams = new LinkedHashMap<String,Object>();

                var iterator = inputParams.keySet().iterator();
                
                
                while (iterator.hasNext()) {
                    
                    String key = iterator.next();                    
                    
                    if (key.equals("obj")) continue; // bypass object key
                    if (key.equals("row_id")) continue; // bypass row_id key                    
                    
                    
                    var a_crudField = crudTable.getField(key);
                    
                    if (a_crudField != null) {
                       t_inputParams.put(key, inputParams.get(key));
                       
                    }
                    
                }
                
                int row_id = Integer.parseInt((String)getParameterOrDefault("row_id", "-1"));   

                
                if (t_inputParams.size() > 0)
                    crudCommonOperations.updateInDatabaseAndGetId(crudObject.getName(),crudTable.getName(), t_inputParams,row_id);       
                
                addToDataModel("message","Data in object \"" + crudObject.getTitle()+ "\" updated !");
                addToDataModel("result", "success");
                renderData(); 
     
     }
    
}
