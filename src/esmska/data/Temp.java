/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author Mizerovi16
 */

 
public class Temp {
        private String template;
         // <editor-fold defaultstate="collapsed" desc="PropertyChange support">
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    // </editor-fold>
        
        public Temp (String template) {
            setTemplate(template);
        }

        public void setTemplate(String template) {
            this.template = template;
        }
        
        public String getTemplate(){
            return this.template;
        }
        
        @Override
        public String toString() {
            return template;
         }
    }
    

