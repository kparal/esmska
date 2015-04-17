/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import esmska.data.event.ActionEventSupport;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Mizerovi16
 */
public class Template {
    
    private static final Template instance = new Template();
    private final List<SMSTemplate> templates = Collections.synchronizedList(new ArrayList<SMSTemplate>());
    private static final Logger logger = Logger.getLogger(Template.class.getName());
    private TemplateChangeListener templateChangeListener = new TemplateChangeListener();
    /** new template added */
    public static final int ACTION_ADD_TEMPLATE = 0;
    /** existing template removed */
    public static final int ACTION_REMOVE_TEMPLATE = 1;
    /** all templates removed */
    public static final int ACTION_CLEAR_TEMPLATES = 2;
    /** text of some template changed */
    public static final int ACTION_CHANGE_TEMPLATE = 3;
    
    
     // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>
    
    
     /** disabled contructor */
    private Template()  {
    }

    /** Get shared instance */
    public static Template getInstance() {
        return instance;
    }
    /** get all template */
    public List<SMSTemplate> getTemplates() {
        return Collections.unmodifiableList(templates);
    }
    
    /** get template at index */
    public SMSTemplate getTemplate(int index) {
        return templates.get(index);
    }
    
    /** add new template
     * @param temp added template*/
    public void addTemplate(SMSTemplate temp) {
        templates.add(temp);
        actionSupport.fireActionPerformed(ACTION_ADD_TEMPLATE, null);
        logger.finer("New template added: " + temp);
    }
    
     /** add new templates */
    public void addTemplates(Collection<SMSTemplate> templates) {
        for (SMSTemplate temp : templates) {
            this.templates.add(temp);
        }
        
        logger.finer(templates.size() + " new templates added");
        actionSupport.fireActionPerformed(ACTION_ADD_TEMPLATE, null);
    }
    
    /** remove existing template */
    public boolean removeTemplate(SMSTemplate temp) {
        if (temp == null) {
            throw new IllegalArgumentException("template");
        }
        logger.fine("Removing template: " + temp);
        boolean removed = false;
        removed = templates.remove(temp);
        if (removed) {
                temp.removePropertyChangeListener(templateChangeListener);
        }
         if (removed) {
            actionSupport.fireActionPerformed(ACTION_REMOVE_TEMPLATE, null);
        }
        return removed;
    }
    
    /** delete all templates */
    public void clear() {     
        templates.clear();
        actionSupport.fireActionPerformed(ACTION_CLEAR_TEMPLATES, null);
        logger.finer("All templates removed");  
    }
    
    public int size() {
        return templates.size();
    }
    
    
     /** Listener for changes in individual templates notifying this class'
     * listeners that some templates properties have changed.
     */
    private class TemplateChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
           actionSupport.fireActionPerformed(ACTION_CHANGE_TEMPLATE, null);
        }
    }
}
