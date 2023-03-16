package com.china.observer.ui;

import com.china.observer.service.MybatisIconGutterService;
import com.china.observer.service.impl.MybatisMapperIconGutterServiceImpl;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

/**
 * 给mapper接口和方法加上跳转图标
 */
public class GutterMapperIcon extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        MybatisIconGutterService iconGutter = new MybatisMapperIconGutterServiceImpl();
        List<RelatedItemLineMarkerInfo<PsiElement>> gutterIconBuilderList = iconGutter.gutterIconHandler(element);
        if (gutterIconBuilderList.size() > 0) {
            result.addAll(gutterIconBuilderList);
        }
    }

}
