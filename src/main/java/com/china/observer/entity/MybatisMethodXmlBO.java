package com.china.observer.entity;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;

public class MybatisMethodXmlBO {

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
    public static MybatisMethodXmlBO buildMethodXmlEntity(PsiElement element, XmlTag xml) {
        MybatisMethodXmlBO mybatisMethodXmlBO = new MybatisMethodXmlBO();
        mybatisMethodXmlBO.setElement(element);
        mybatisMethodXmlBO.setXml(xml);
        return mybatisMethodXmlBO;
    }

}
