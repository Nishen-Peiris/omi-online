/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Nishen K Peiris
 */
public enum Status {
    Loading("loading"),
    Waiting("waiting"),
    Play("play");
    
    private final String status;
    
    Status(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
