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

package org.acceix.frontend.core.accounts;

import org.acceix.frontend.menu.MenuManager;
import org.acceix.frontend.helpers.ActionSettings;
import org.acceix.frontend.helpers.ModuleHelper;
import org.acceix.frontend.web.commons.FrontendSecurity;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.acceix.ndatabaseclient.exceptions.MachineDataException;
import org.acceix.logger.NLog;
import org.acceix.logger.NLogBlock;
import org.acceix.logger.NLogger;
import org.acceix.ndatabaseclient.dataset.MachineDataSet;


public class Accounts extends org.acceix.frontend.helpers.ModuleHelper {


        @Override
        public void construct() {
            
                setModuleName("accounts");
                addAction(new ActionSettings("signinpage", false,this::signinpage));
                addAction(new ActionSettings("signuppage", false,this::signuppage));
                addAction(new ActionSettings("auth", false,this::auth));
                addAction(new ActionSettings("deauth", false,this::deauth));
                addAction(new ActionSettings("gettoken", false,this::gettoken));
                addAction(new ActionSettings("signup", false,this::signup));      
                addAction(new ActionSettings("gotoMainPageOrSigninAgain", false,this::gotoMainPageOrSigninAgain));

        }
        
        
        public ModuleHelper getInstance() {
            return new Accounts();
        }
        
    
        public void signup() {
        
                Map<String,Object> params = getRequestObject().getParams();
                

                
                String username = (String) getParameterOrDefault("username","");
                String password = (String) getParameterOrDefault("password","");

                
                if (username.isEmpty() || password.isEmpty()) {
                    addToDataModel("result", "error");
                    addToDataModel("message", "Wrong inputs !");
                    renderData();
                }
                
            
        }
   
        
        
        
        public void auth ()  {
            
            String domain = (String) getParameter("domain");
            String username = (String) getParameter("username");
            String password = (String) getParameter("password"); 
            
            
            try {
             
                if (domain==null) {
                    domain = getRequestedDomain();
                }
                               

                int domain_id = getDatabaseAdminFunctions().getIdOfDomain(domain);
                
                if (username.equals(getGlobalEnvs().get("admin_user")) ) {
                    if (password.equals(getGlobalEnvs().get("admin_password"))) {
                        if (domain.equals(getGlobalEnvs().get("admin_domain"))) {
                            NLogger.logger(NLogBlock.WEB,NLog.SECURITY,getModuleName(),"auth",username,"Admin login:[username:" + username  + " domain:" + domain + " domain_token:" + domain_id);
                            setUserId(0);
                        } else {
                            setUserId(-1);
                        }
                    } else {
                        setUserId(-1);
                    }
                } else {
                    NLogger.logger(NLogBlock.WEB,NLog.SECURITY,getModuleName(),"auth",username, "Login:[username:" + username + " domain:" + domain + " domain_token:" + domain_id);
                    setUserId(getDatabaseAdminFunctions().getUserId(username, password,domain_id, 1));
                }
                
               
                if ( getUserId() > -1 ) {
                    
                        String main_page = getDatabaseAdminFunctions().getMainpageOfDomain(domain);
                        
                        if (main_page==null) {
                            setMainPage((String)getGlobalEnvs().get("default_main_page"));
                        } else {
                            setMainPage(main_page);
                        }
                        
                        createSession();

                        
                        try {
                        
                            String token = getTokenByUserId(getUserId());
                            
                            if (token !=null) {

                                setSessionAttribute("authenticated", true);
                                setSessionAttribute("userid",getUserId());  
                                setSessionAttribute("token", token); 
                                setupUserEnvironments();
                                
                            } else {
                                setSessionAttribute("authenticated", false);
                            }
                            

                        } catch (MachineDataException ex) {
                            Logger.getLogger(Accounts.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        getDatabaseAdminFunctions().addActivityLog(1, getUserId(), getRequestIP(), 3, getRequestHeader("USER-AGENT"));

                        addToDataModel("menu", new MenuManager(getGlobalEnvs(),getUsername()).menu(getRolesOfUser()));  
                        addToDataModel("default_object_to_load", getParameterOrDefault("default_object_to_load", null));
                        addToDataModel("default_module_to_load", getParameterOrDefault("default_module_to_load", null));
                        addToDataModel("default_action_to_load", getParameterOrDefault("default_action_to_load", null));
                        renderData("/" + getMainPage());                         
                    

                } else {
                    
                        setSessionAttribute("authenticated",false);

                        addToDataModel("autherror", "Username or Password incorrect !");
                        signinpage();
                    
                }
                
                //gotoMainPageOrSigninAgain();
                
            } catch (ClassNotFoundException | SQLException | IOException | MachineDataException | NoSuchAlgorithmException ex) {
                Logger.getLogger(Accounts.class.getName()).log(Level.SEVERE, null, ex);
            }
                        
            
            
        }
        
        public void gotoMainPageOrSigninAgain() {
            
            setSession();
            
            if ( (isAuthenticatedByToken() || isUserAuthenticatedBySession()) && ( (int)getSessionAttribute("userid") >= 0 ) ){
                
                    try {
                        setUserId((int)getSessionAttribute("userid"));
                        setupUserEnvironments();
                    } catch (MachineDataException | ClassNotFoundException | SQLException ex) {
                        Logger.getLogger(ModuleHelper.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    try {
                        
                        /* pass all input to frontend 
                        getRequestObject().getParams().forEach( (paramName,paramValue) -> {
                            addToDataModel(paramName, paramValue);
                        }); */
                        addToDataModel("menu", new MenuManager(getGlobalEnvs(),getUsername()).menu(getRolesOfUser()));
                        
                        addToDataModel("default_object_to_load", getParameterOrDefault("default_object_to_load", null));
                        addToDataModel("default_module_to_load", getParameterOrDefault("default_module_to_load", null));
                        addToDataModel("default_action_to_load", getParameterOrDefault("default_action_to_load", null)); 
                        
                        String main_page = getDatabaseAdminFunctions().getMainpageOfDomain(getDomain());
                        
                        
                        if (main_page==null) {
                            setMainPage((String)getGlobalEnvs().get("default_main_page"));
                        } else {
                            setMainPage(main_page);
                        }                        
                        
                         
                        renderData("/" + main_page);
                    } catch (IOException ex) {
                        Logger.getLogger(Accounts.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(Accounts.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (SQLException ex) {
                        Logger.getLogger(Accounts.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
            } else {
                signinpage();
            }
            
        }
        
        public void deauth() {
            
                
                setSession();
            
                //if (isUserAuthenticatedBySession()) {
                
                    setSessionAttribute("authenticated",false);
                    setSessionAttribute("userid",-1);
                    addToDataModel("authenticated", false);
                    addToDataModel("domain", getDomain());
                    signinpage();
                    
                //}
                
        }
        
       
        
        public void signinpage() {
            
                if (isUserAuthenticatedBySession()) {
                    try {
                        
                        addToDataModel("menu", new MenuManager(getGlobalEnvs(),getUsername()).menu(getRolesOfUser()));
                        
                        addToDataModel("defaultpage", getParameterOrDefault("defaultpage", null));   
                        addToDataModel("default_container_to_load", getParameterOrDefault("default_container_to_load", null));  

                        
                        renderData("/" + getMainPage());
                        
                    } catch (IOException ex) {
                        Logger.getLogger(Accounts.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return;
                }
            
                String domain = (String) getParameter("domain");

                if ( (domain==null) || (domain.equals("")) ) {
                    domain = getRequestedDomain();
                }

                try {



                    String signInPage;
                    
                    
                    if (domain.equals(getGlobalEnvs().get("admin_domain"))) {
                        signInPage = "signin_admin";
                    } else {
                        signInPage = getDatabaseAdminFunctions().getSignInPageOfDomain(domain);
                    }
                    
                    
                    addToDataModel("domain", domain);  
                                        


                        if (signInPage==null) {
                            renderData((String)getGlobalEnvs().get("wrong_domain_view_file"));
                        } else {
                            addToDataModel("title", getDatabaseAdminFunctions().getTitleOfDomain(domain));                            
                            renderData("/" + signInPage);                        
                        }
                        
                } catch (ClassNotFoundException | SQLException ex) {
                    addToDataModel("result","error");
                    addToDataModel("message", "internal error  code 1984!");    
                    renderData();
                    Logger.getLogger(Accounts.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                Logger.getLogger(Accounts.class.getName()).log(Level.SEVERE, null, ex);
            }

                
        }
        
        
        public void signuppage() {
            
            
                String domain = (String) getParameter("domain");
                
                if ( (domain==null) || (domain.equals("")) ) {
                    domain = getRequestedDomain();
                }
                
                

                setContentType("text/html;charset=UTF-8");
                String domain_token;
                try {
                    
                    domain_token = getDatabaseAdminFunctions().getTokenOfDomain(domain);
                    
                    String signuppage = getDatabaseAdminFunctions().getSignUpPageOfDomain(domain);
                    
                        if (domain_token!=null) {
                            addToDataModel("domain_token",domain_token);
                        } else {
                            addToDataModel("result","error");
                            addToDataModel("msg", "Internal domain error , wrong requested !");
                            renderData("/" + signuppage);
                            return;
                        }
                        
                addToDataModel("domain", domain);
                renderData("/" + signuppage);                        
                        
                } catch (ClassNotFoundException | SQLException ex) {
                    addToDataModel("domain_token", "error2");                    
                    Logger.getLogger(Accounts.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                Logger.getLogger(Accounts.class.getName()).log(Level.SEVERE, null, ex);
            }

                
        }   
        

        

        
        public void gettoken () {
       
                        initNewDataModel();
                    
                        String username = (String) getParameter("username");
                        String password = (String) getParameter("password"); 
                        String domain_token = (String) getParameter("domain_token");

                        
                        
                        
                        int userid = -1;
                        int domain_id = -1;
                                
                        try {
                            
                        
                            if (domain_token==null) {
                                String req_domain = getHttpServletRequest().getServerName();
                                domain_id = getDatabaseAdminFunctions().getIdOfDomain(req_domain);
                            } else {                            
                                domain_id = getDatabaseAdminFunctions().getIdOfDomain(getDatabaseAdminFunctions().getDomainByToken(domain_token));
                            }
                            userid = getDatabaseAdminFunctions().getUserId(username, password,domain_id, 1);
                        } catch (ClassNotFoundException | SQLException | NoSuchAlgorithmException ex) {
                            Logger.getLogger(Accounts.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        if ( userid > -1 ) {
                            
            
                            MessageDigest md5 = null;
                            try {
                                md5 = MessageDigest.getInstance("MD5"); // you can change it to SHA1 if needed!
                            } catch (NoSuchAlgorithmException ex) {
                                Logger.getLogger(Accounts.class.getName()).log(Level.SEVERE, null, ex);
                            }

                                md5.update(password.getBytes(), 0, password.length());

                                String token = generateToken(userid, new BigInteger(1, md5.digest()).toString(16)  , domain_id);
                                
                                    if (token != null) {

                                            setContentType("text/html;charset=UTF-8"); 
                                            getOutputWriter().print(token);
                                            getOutputWriter().flush();
                                            return;
                                        
                                        
                                    } else {
                                        setSessionAttribute("authenticated",false);
                                    }
                             
                            
                        } else {
                                setSessionAttribute("authenticated",false);                                
                        }  
                        
                        if (getSessionAttribute("authenticated")!=null) {
                            if (!(Boolean)getSessionAttribute("authenticated")) {

                                    setContentType("text/html;charset=UTF-8");
                                    getOutputWriter().print("false");
                                    getOutputWriter().flush();

                            }
                        }
            
            
        }
        
        public String getTokenByUserId(int user_id) throws MachineDataException {
            
                if (user_id==0) return "ADMINTOKENPROHIBITED";
            
                MachineDataSet machineDataSet =  getDatabaseAdminFunctions().getUserInfo(user_id);
                
                if (machineDataSet.next()) {
                
                    String password = machineDataSet.getFirstString("password");
                    int domain_id = machineDataSet.getFirstInt("domain_id");

                    return generateToken(user_id, password, domain_id);
                
                } else {
                    return null;
                }
            
        }    
    
        public String generateToken (int userid,String password,int domain_id) {
            

                                String token;
                                
                                String tokenOfUserId =  Base64.getEncoder().encodeToString(String.valueOf(userid).getBytes());

                                    try {
                                        
                                            MessageDigest m = MessageDigest.getInstance("MD5");
                                            m.update(password.getBytes(),0,password.length());

                                            String tokenOfPassword = Base64.getEncoder().encodeToString(new BigInteger(1,m.digest()).toString(16).getBytes());

                                            token = tokenOfUserId + ":" + tokenOfPassword;


                                            return token;
                                        
                                        
                                    } catch (NoSuchAlgorithmException ex) {
                                        Logger.getLogger(FrontendSecurity.class.getName()).log(Level.SEVERE, null, ex);                                        
                                        return null;
                                    }
                             
       
        }            
      
       
                

    
}
