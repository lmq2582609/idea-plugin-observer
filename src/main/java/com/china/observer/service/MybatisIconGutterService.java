package com.china.observer.service;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import java.util.List;

public interface MybatisIconGutterService {

    /**
     * 处理gutter区域图标
     * @param element
     * @return
     */
    List<RelatedItemLineMarkerInfo<PsiElement>> gutterIconHandler(PsiElement element);

}
