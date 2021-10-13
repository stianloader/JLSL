package org.jglrxavpok.jlsl.fragments;

import java.util.ArrayList;
import java.util.HashMap;

public class StartOfMethodFragment extends CodeFragment {
    public AccessPolicy access;
    public String name;
    public String owner;
    public String returnType;
    public ArrayList<String> argumentsTypes = new ArrayList<>();
    public ArrayList<String> argumentsNames = new ArrayList<>();
    public HashMap<Integer, String> varNameMap = new HashMap<>();
    public HashMap<Integer, String> varTypeMap = new HashMap<>();
    public HashMap<String, String> varName2TypeMap = new HashMap<>();
}
