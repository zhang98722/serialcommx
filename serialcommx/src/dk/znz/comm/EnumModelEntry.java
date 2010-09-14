/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.znz.comm;

/**
 *
 * @author Esben
 */
public class EnumModelEntry {

    private Object value;
    private String string;

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public EnumModelEntry(Object value, String string) {
        this.value = value;
        this.string = string;
    }

    @Override
    public String toString() {
        return string;
    }

}
