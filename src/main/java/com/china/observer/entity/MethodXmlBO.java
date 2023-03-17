package com.china.observer.entity;

import com.china.observer.util.PsiUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
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
     * 构建实体类
     * @param element
     * @param xml
     * @return
     */
    public static MethodXmlBO buildMethodXmlEntity(PsiElement element, XmlTag xml) {
        MethodXmlBO methodXmlBO = new MethodXmlBO();
        methodXmlBO.setElement(element);
        methodXmlBO.setXml(xml);
        return methodXmlBO;
    }
}
