package com.china.observer.entity;

import com.china.observer.util.PsiUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

public class MethodXmlBO {

    /**
     * mapper接口
     */
    private PsiElement element;

    /**
     * mapper接口的方法对应的xml
     */
    private XmlTag xml;

    public PsiElement getElement() {
        return element;
    }

    public void setElement(PsiElement element) {
        this.element = element;
    }

    public XmlTag getXml() {
        return xml;
    }

    public void setXml(XmlTag xml) {
        this.xml = xml;
    }

    /**
     * 检查是否是mapper，返回xml根节点
     * @param virtualFile
     * @param containingClass
     * @return
     */
    public static XmlTag checkIsMapper(VirtualFile virtualFile, PsiClass containingClass) {
        //如果不是一个接口，跳过
        if (containingClass == null || !containingClass.isInterface()) {
            return null;
        }
        PsiManager psiManager = PsiManager.getInstance(containingClass.getProject());
        PsiFile file = psiManager.findFile(virtualFile);
        if (!(file instanceof XmlFile)) {
            return null;
        }
        XmlFile xmlFile = (XmlFile) file;
        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag == null || !"mapper".equals(rootTag.getName())){
            return null;
        }
        //检查这个class是否为第三方
        String packageName1 = PsiUtil.getPackageName(containingClass);
        //包路径
        String packageName = ((PsiJavaFileImpl) containingClass.getContainingFile()).getPackageName() + "." + containingClass.getName();
        //检查这个class与xml中的namespace是否一致
        String namespace = rootTag.getAttributeValue("namespace");
        //校验不一致，跳过
        if (!packageName.equals(namespace)) {
            return null;
        }
        return rootTag;
    }
    public static MethodXmlBO buildMethodXmlEntity(PsiElement element, XmlTag xml) {
        MethodXmlBO methodXmlBO = new MethodXmlBO();
        methodXmlBO.setElement(element);
        methodXmlBO.setXml(xml);
        return methodXmlBO;
    }
}
