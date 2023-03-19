package com.china.observer.service;

import com.intellij.psi.PsiElement;

/**
 * Mybatis Mapper接口与xml关联关系
 */
public interface MybaitsMapperCorrXmlService {

    /**
     * 根据element查找对应的关联关系，element参数可能是PsiMethod或PsiClass
     * @param psiElement
     * @return
     */
    PsiElement mapperCorrelationXml(PsiElement psiElement);

}
