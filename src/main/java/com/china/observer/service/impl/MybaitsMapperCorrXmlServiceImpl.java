package com.china.observer.service.impl;

import com.china.observer.service.MybaitsMapperCorrXmlService;
import com.china.observer.util.MybatisUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

public class MybaitsMapperCorrXmlServiceImpl implements MybaitsMapperCorrXmlService {

    /**
     * 根据element查找对应的关联关系，element参数可能是PsiMethod或PsiClass
     * @param psiElement
     * @return
     */
    @Override
    public PsiElement mapperCorrelationXml(PsiElement psiElement) {
        //如果是一个mapper接口
        if (psiElement instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) psiElement;
            //如果不是接口，跳过
            if (!psiClass.isInterface()) {
                return null;
            }
            return MybatisUtil.findXmlByPsiClass(psiClass);
        }
        //如果是一个mapper接口中的函数
        if (psiElement instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod) psiElement;
            PsiClass containingClass = psiMethod.getContainingClass();
            //如果不是接口，跳过
            if (containingClass == null || !containingClass.isInterface()) {
                return null;
            }
            return MybatisUtil.findXmlByPsiMethod(psiMethod);
        }
        return null;
    }

}
