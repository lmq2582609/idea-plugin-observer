package com.china.observer.extensions;

import com.china.observer.service.MybatisIconGutterService;
import com.china.observer.service.impl.MybatisXmlIconGutterServiceImpl;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.List;

/**
 * 给Mybatis的Mapper.xml加上图标
 */
public class MybatisXmlGutterIcon extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        MybatisIconGutterService iconGutterService = new MybatisXmlIconGutterServiceImpl();
        List<RelatedItemLineMarkerInfo<PsiElement>> gutterIconBuilderList = iconGutterService.gutterIconHandler(element);
        if (gutterIconBuilderList.size() > 0) {
            result.addAll(gutterIconBuilderList);
        }
    }

}
