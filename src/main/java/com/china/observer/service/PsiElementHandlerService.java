package com.china.observer.service;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.xml.XmlTag;

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

    /**
     * 检查方法与xml关联
     * @param pm
     * @return
     */
    XmlTag selectMethodRelationXml(PsiMethod pm);

    /**
     * 检查是否为mapper，是的话返回这个节点
     * @param virtualFile
     * @param containingClass
     * @return
     */
    XmlTag checkIsMapper(VirtualFile virtualFile, PsiClass containingClass);

}
