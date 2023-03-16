package com.china.observer.service.impl;

import com.china.observer.service.PsiElementHandlerService;
import com.intellij.psi.*;
import java.util.Objects;
import java.util.Optional;

public class PsiElementHandlerServiceImpl implements PsiElementHandlerService {

    @Override
    public PsiField checkMethodIsGetterOrSetter(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return null;
        }
        String methodName = method.getName();

        if (methodName.startsWith("get")) {
            String fieldName = methodName.substring(3);
            PsiType returnType = method.getReturnType();
            if (Objects.isNull(returnType)) {
                return null;
            }
            //get方法返回结果的类型与字段类型一致
            PsiField psiField = selectClassField(containingClass, fieldName);
            return Optional.ofNullable(psiField)
                    .filter(field -> field.getType().getCanonicalText().equals(returnType.getCanonicalText()))
                    .orElse(null);
        }
        if (methodName.startsWith("set")) {
            //检查参数
            PsiParameter[] parameters = method.getParameterList().getParameters();
            if (parameters.length != 1) {
                return null;
            }
            String fieldName = methodName.substring(3);
            PsiParameter parameter = parameters[0];
            //set方法的参数类型与字段类型一致
            PsiField psiField = selectClassField(containingClass, fieldName);
            return Optional.ofNullable(psiField)
                    .filter(field -> field.getType().getCanonicalText().equals(parameter.getType().getCanonicalText()))
                    .orElse(null);
        }
        return null;
    }

    /**
     * 查找类中的字段
     * @param psiClass
     * @param fieldName
     * @return
     */
    private PsiField selectClassField(PsiClass psiClass, String fieldName) {
        //首字母转小写
        String field = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
        //先使用findFiledByName查找一遍
        PsiField psiField = psiClass.findFieldByName(field, true);
        if (psiField != null) {
            return psiField;
        }
        //未查找到，再遍历查找一遍，但此处不能查找父类字段
        PsiField[] fields = psiClass.getFields();
        fieldName = fieldName.toLowerCase();
        for (PsiField f : fields) {
            if (f.getName().toLowerCase().equals(fieldName)) {
                return f;
            }
        }
        return null;
    }

}
