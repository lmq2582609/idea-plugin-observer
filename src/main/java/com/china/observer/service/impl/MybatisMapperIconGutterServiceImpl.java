package com.china.observer.service.impl;

import com.china.observer.entity.MybatisMethodXmlBO;
import com.china.observer.service.MybatisIconGutterService;
import com.china.observer.util.MybatisUtil;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.xml.XmlTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * mybatis mapper接口图标处理
 */
public class MybatisMapperIconGutterServiceImpl implements MybatisIconGutterService {

    @Override
    public List<RelatedItemLineMarkerInfo<PsiElement>> gutterIconHandler(PsiElement element) {
        //如果不是接口，跳过
        if (!(element instanceof PsiClass)) {
            return Collections.emptyList();
        }
        PsiClass psiClass = (PsiClass) element;
        //查找到mapper接口对应的XML根节点
        XmlTag rootXmlTag = MybatisUtil.findXmlByPsiClass(psiClass);
        if (rootXmlTag == null) {
            return Collections.emptyList();
        }
        //查找并构建mapper与xml之间的关系
        List<MybatisMethodXmlBO> mybatisMethodXmlBOS = MybatisUtil.buildMapperMethodCorrXml(rootXmlTag.getSubTags(), psiClass.getChildren());
        //接口与xml根节点跳转位置已确定
        MybatisMethodXmlBO entity = MybatisMethodXmlBO.buildMethodXmlEntity(psiClass, rootXmlTag);
        mybatisMethodXmlBOS.add(entity);
        //构建图标
        return buildNavigationGutterIcon(mybatisMethodXmlBOS);
    }

    /**
     * 构建图标
     * @param mybatisMethodXmlBOs
     * @return
     */
    private List<RelatedItemLineMarkerInfo<PsiElement>> buildNavigationGutterIcon(List<MybatisMethodXmlBO> mybatisMethodXmlBOs) {
        if (mybatisMethodXmlBOs == null || mybatisMethodXmlBOs.size() == 0) {
            return Collections.emptyList();
        }
        NavigationGutterIconBuilder<PsiElement> builder =
                NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
                        .setAlignment(GutterIconRenderer.Alignment.RIGHT);
        List<RelatedItemLineMarkerInfo<PsiElement>> resultMarkerInfoList = new ArrayList<>();
        for (MybatisMethodXmlBO mybatisMethodXmlBO : mybatisMethodXmlBOs) {
            //目标 -> xml节点
            builder.setTarget(mybatisMethodXmlBO.getXml());
            //图标文字
            builder.setTooltipText("Go To " + mybatisMethodXmlBO.getXml().getName());
            PsiElement nameIdentifier = ((PsiNameIdentifierOwner) mybatisMethodXmlBO.getElement()).getNameIdentifier();
            if (nameIdentifier != null) {
                resultMarkerInfoList.add(builder.createLineMarkerInfo(nameIdentifier));
            }
        }
        return resultMarkerInfoList;
    }

}
