package com.china.observer.ui;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 给XML加上图标
 */
public class GutterMapperXmlIcon extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        // 检查元素是否是XML标记
        if (!(element instanceof XmlTag)) {
            return;
        }
        // 获取XML标记
        XmlTag xmlTag = (XmlTag) element;
        if (!"mapper".equals(xmlTag.getName())){
            return;
        }
        //xml的namespace
        String namespace = xmlTag.getAttributeValue("namespace");
        if (namespace == null || "".equals(namespace)){
            return;
        }
        //查找项目中所有Java文件
        Collection<VirtualFile> projectAllXML = FileTypeIndex.getFiles(JavaFileType.INSTANCE,
                GlobalSearchScope.projectScope(xmlTag.getProject()));
        //未查找到Java文件，跳过
        if (projectAllXML.size() == 0) {
            return;
        }
        //根据namespace获取class
        PsiClass psiClass = JavaPsiFacade.getInstance(xmlTag.getProject())
                .findClass(namespace, GlobalSearchScope.allScope(xmlTag.getProject()));
        if (psiClass == null || !psiClass.isInterface()) {
            return;
        }
        //接口内所有防范
        PsiMethod[] allMethods = psiClass.getAllMethods();
        //存储方法与xml的对应关系
        Map<XmlTag, PsiMethod> xmlMethodMap = new HashMap<>();
        XmlTag[] subTags = xmlTag.getSubTags();
        for (XmlTag subTag : subTags) {
            String id = subTag.getAttributeValue("id");
            for (PsiMethod method : allMethods) {
                if (method.getName().equals(id)) {
                    xmlMethodMap.put(subTag, method);
                }
            }
        }
        //给xml根节点添加按钮
        if (psiClass.getNameIdentifier() != null) {
            NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementingMethod)
                            .setTarget(psiClass)
                            .setAlignment(GutterIconRenderer.Alignment.RIGHT)
                            .setTooltipText("Go To " + psiClass.getName());
            result.add(builder.createLineMarkerInfo(xmlTag));
        }
        //给xml添加按钮
        if (xmlMethodMap.size() > 0) {
            NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementingMethod)
                            .setAlignment(GutterIconRenderer.Alignment.RIGHT);
            for (Map.Entry<XmlTag, PsiMethod> entry : xmlMethodMap.entrySet()) {
                builder.setTarget(entry.getValue())
                        .setTooltipText("Go To " + entry.getValue().getName());
                result.add(builder.createLineMarkerInfo(entry.getKey()));
            }
        }
    }

}
