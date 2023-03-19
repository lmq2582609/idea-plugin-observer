package com.china.observer.service.impl;

import com.china.observer.service.PsiElementHandlerService;
import com.china.observer.util.PsiElementUtil;
import com.intellij.psi.*;
import java.util.Optional;

public class PsiElementHandlerServiceImpl implements PsiElementHandlerService {

    /**
     * 检查是否为get或set方法
     * @param method
     * @return
     */
    @Override
    public PsiField checkMethodIsGetterOrSetter(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return null;
        }
        //get开头的方法
        if (method.getName().startsWith("get")) {
            //截断get，取后面的字符串为字段名称
            String fieldName = method.getName().substring(3);
            //方法返回值类型
            PsiType returnType = method.getReturnType();
            if (returnType == null) {
                return null;
            }
            //字段名首字母转小写
            fieldName = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
            //查找类中的字段
            PsiField psiField = PsiElementUtil.selectClassField(containingClass, fieldName);
            //get方法返回结果的类型与字段类型一致
            return Optional.ofNullable(psiField)
                    .filter(field -> field.getType().getCanonicalText().equals(returnType.getCanonicalText()))
                    .orElse(null);
        }
        //set开头的方法
        if (method.getName().startsWith("set")) {
            //检查参数，set方法只有1个参数
            PsiParameter[] parameters = method.getParameterList().getParameters();
            if (parameters.length != 1) {
                return null;
            }
            //截断set，取后面的字符串为字段名称
            String fieldName = method.getName().substring(3);
            PsiParameter parameter = parameters[0];
            //查找类中的字段
            PsiField psiField = PsiElementUtil.selectClassField(containingClass, fieldName);
            //set方法的参数类型与字段类型一致
            return Optional.ofNullable(psiField)
                    .filter(field -> field.getType().getCanonicalText().equals(parameter.getType().getCanonicalText()))
                    .orElse(null);
        }
        return null;
    }

}
