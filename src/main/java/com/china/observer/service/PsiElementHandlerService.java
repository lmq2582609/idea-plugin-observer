package com.china.observer.service;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;

/**
 * psi处理
 */
public interface PsiElementHandlerService {

    PsiField checkMethodIsGetterOrSetter(PsiMethod method);

}
