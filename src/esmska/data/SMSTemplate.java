package esmska.data;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author Mizerovi16
 */

 
public class SMSTemplate {
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
        
        public SMSTemplate (String template) {
            setTemplate(template);
        }
        
        public SMSTemplate (SMSTemplate template){
            setTemplate(template.toString());
        }

        public void copyFrom(SMSTemplate t) {
        setTemplate(t.getTemplate());
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
    

