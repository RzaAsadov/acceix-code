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

import org.acceix.frontend.helpers.ActionSettings;
import org.acceix.frontend.helpers.ModuleHelper;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Setup extends org.acceix.frontend.helpers.ModuleHelper {


        @Override
        public void construct() {
            
                setModuleName("setup");
                addAction(new ActionSettings("databasepage", false,this::databasepage));


        }
        
        
        public ModuleHelper getInstance() {
            return new Setup();
        }
        

       
        
        public void databasepage() {
            

            
                String domain =  getRequestedDomain();


                try {



                    String signInPage = "setup";
                    
 
                    
                    addToDataModel("domain", domain);  

                            addToDataModel("title", "Setup page");                            
                            
                            renderData("/" + signInPage);                        

                        
                } catch (IOException ex) {
                    Logger.getLogger(Setup.class.getName()).log(Level.SEVERE, null, ex);
                }

                
        }
        
        

    
}
