package org.dtls.fairifier;

/**
 * A adapter which takes a resource and pushes it.
 * @author Shamanou van Leeuwen
 * @date 28-11-2016
 *
 */
public class PushFairDataToResourceAdapter {
    private Resource resource;
    
    public void push(){
        this.resource.push();
    }
    
    public void setResource(Resource resource){
        this.resource = resource;
    }
}
