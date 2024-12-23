package com.example.sample.flow;

import soot.*;
import soot.jimple.*;

public class FieldInitializationChecker {
    public static boolean checkFieldInitialization(SootField field, SootClass sootClass, InitializationInfo info) {
        boolean isInitialized = false;

        // クラス初期化子 (<clinit>) の確認
        if (sootClass.declaresMethodByName("<clinit>")) {
            SootMethod clinit = sootClass.getMethodByName("<clinit>");
            isInitialized = checkMethodForFieldInitialization(field, clinit, info);
        }

        // コンストラクタ (<init>) の確認
        for (SootMethod method : sootClass.getMethods()) {
            if (method.isConstructor()) {
                isInitialized |= checkMethodForFieldInitialization(field, method, info);
            }
        }

        return isInitialized;
    }

    private static boolean checkMethodForFieldInitialization(SootField field, SootMethod method, InitializationInfo info) {
        boolean initialized = false;
        Body body = method.retrieveActiveBody();

        for (Unit unit : body.getUnits()) {
            if (unit instanceof AssignStmt) {
                AssignStmt assign = (AssignStmt) unit;
                Value leftOp = assign.getLeftOp();

                // フィールドが初期化されているか確認
                if (leftOp instanceof InstanceFieldRef || leftOp instanceof StaticFieldRef) {
                    SootFieldRef fieldRef = (leftOp instanceof InstanceFieldRef)
                            ? ((InstanceFieldRef) leftOp).getFieldRef()
                            : ((StaticFieldRef) leftOp).getFieldRef();

                    if (fieldRef.name().equals(field.getName())) {
                        initialized = true;

                        // 初期化情報を設定
                        if (info != null) {
                            info.setUnit(unit);
                            info.setMethod(method);
                            info.setInitializationValue(assign.getRightOp()); // 初期化値をセット
                        }

                        System.out.println("  Initialized in method: " + method.getSignature() + ", at unit: " + ((AssignStmt) unit).getRightOp());
                        break;
                    }
                }
            }
        }
        return initialized;
    }
}

