package com.china.observer.service.impl;

import com.china.observer.entity.MybatisMethodXmlBO;
import com.china.observer.service.MybatisIconGutterService;
import com.china.observer.util.MybatisUtil;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlTag;
import java.util.*;

/**
 * mybatis xml图标处理
 */
public class MybatisXmlIconGutterServiceImpl implements MybatisIconGutterService {

    /**
     * 处理xml gutter区域图标
     * @param element
     * @return
     */
    @Override
    public List<RelatedItemLineMarkerInfo<PsiElement>> gutterIconHandler(PsiElement element) {
        //如果不是xml，跳过
        if (!(element instanceof XmlTag)) {
            return Collections.emptyList();
        }
        //获取xml
        XmlTag xmlTag = (XmlTag) element;
        //根据xml查找对应的class
        PsiClass psiClass = MybatisUtil.findPsiClassByXmlTag(xmlTag);
        if (psiClass == null) {
            return Collections.emptyList();
        }
        //查找并构建mapper与xml之间的关系
        List<MybatisMethodXmlBO> mybatisMethodXmlBOS = MybatisUtil.buildMapperMethodCorrXml(xmlTag.getSubTags(), psiClass.getAllMethods());
        //接口与xml根节点跳转位置已确定
        MybatisMethodXmlBO entity = MybatisMethodXmlBO.buildMethodXmlEntity(psiClass, xmlTag);
        mybatisMethodXmlBOS.add(entity);
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
                NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementingMethod)
                        .setAlignment(GutterIconRenderer.Alignment.RIGHT);
        List<RelatedItemLineMarkerInfo<PsiElement>> resultMarkerInfoList = new ArrayList<>();
        for (MybatisMethodXmlBO mybatisMethodXmlBO : mybatisMethodXmlBOs) {
            //目标 -> xml节点
            builder.setTarget(mybatisMethodXmlBO.getElement());
            //图标文字
            builder.setTooltipText("Go To " + ((PsiNameIdentifierOwner) mybatisMethodXmlBO.getElement()).getName());
            resultMarkerInfoList.add(builder.createLineMarkerInfo(mybatisMethodXmlBO.getXml()));
        }
        return resultMarkerInfoList;
    }

}
