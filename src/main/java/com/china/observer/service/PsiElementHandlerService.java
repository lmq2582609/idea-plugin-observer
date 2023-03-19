package com.china.observer.service;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;

/**
 * psi处理
 */
public interface PsiElementHandlerService {

    /**
     * 检查是否为get或set方法
     * @param method
     * @return
     */
    PsiField checkMethodIsGetterOrSetter(PsiMethod method);

}
