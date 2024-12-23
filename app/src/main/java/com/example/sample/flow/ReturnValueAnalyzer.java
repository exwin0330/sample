package com.example.sample.flow;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.ReturnStmt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReturnValueAnalyzer {

    /**
     * メソッドの最後のreturn文を解析し、返り値を取得します。
     *
     * @param method 解析対象のSootMethod
     * @return 返り値オブジェクト（例: 0, "text", nullなど）
     */
    public static Object analyzeReturnValue(SootMethod method) {
        // メソッドが有効で、ボディを持っているか確認
        if (!method.hasActiveBody()) {
            System.out.println("No active body for method: " + method.getName());
            return null;
        }

        Body body = method.getActiveBody();
        // UnitPatchingChainをリストに変換
        List<Unit> units = new ArrayList<>();
        Iterator<Unit> iterator = body.getUnits().iterator();
        while (iterator.hasNext()) {
            units.add(iterator.next());
        }

        // 逆順にUnitを辿る
        for (int i = units.size() - 1; i >= 0; i--) {
            Unit unit = units.get(i);

            // return文をチェック
            if (unit instanceof ReturnStmt) {
                ReturnStmt returnStmt = (ReturnStmt) unit;
                Value returnValue = returnStmt.getOp();

                // 値が定数の場合
                if (returnValue instanceof IntConstant) {
                    return ((IntConstant) returnValue).value;
                }

                // 他の型にも対応
                // ここで必要に応じて型を確認し、処理を追加できます
                return returnValue.toString();
            }
        }

        // return文が見つからなかった場合
        System.out.println("No return statement found in method: " + method.getName());
        return null;
    }
}
