package com.softbankrobotics.pepperapplauncher;

import java.util.Observable;

public class ObservableObject extends Observable {
    private static ObservableObject instance = new ObservableObject();

    private ObservableObject() {
    }

    public static ObservableObject getInstance() {
        return instance;
    }

}