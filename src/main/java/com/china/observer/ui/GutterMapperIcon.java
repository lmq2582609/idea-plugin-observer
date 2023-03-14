package com.china.observer.ui;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import java.util.*;

/**
 * 给mapper接口和方法加上跳转图标
 */
public class GutterMapperIcon extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        //如果不是接口，跳过
        if (!(element instanceof PsiClass) || !((PsiClass) element).isInterface()) {
            return;
        }
        PsiClass psiClass = (PsiClass) element;
        //将当前class与xml进行匹配
        PsiManager psiManager = PsiManager.getInstance(psiClass.getProject());
        //查找项目中所有XML文件
        Collection<VirtualFile> projectAllXML = FileTypeIndex.getFiles(XmlFileType.INSTANCE,
                GlobalSearchScope.projectScope(psiClass.getProject()));
        //未查找到xml文件，跳过
        if (projectAllXML.size() == 0) {
            return;
        }
        //接口的跳转位置
        XmlTag interfaceTarget = null;
        //方法的跳转位置 -> xml，建立1对1关系
        Map<PsiMethod, XmlTag> psiMethodXmlMap = new HashMap<>();
        for (VirtualFile virtualFile : projectAllXML) {
            PsiFile file = psiManager.findFile(virtualFile);
            if (!(file instanceof XmlFile)) {
                continue;
            }
            XmlFile xmlFile = (XmlFile) file;
            XmlTag rootTag = xmlFile.getRootTag();
            if (rootTag == null || !"mapper".equals(rootTag.getName())){
                continue;
            }
            //包路径
            String packageName = ((PsiJavaFileImpl) psiClass.getContainingFile()).getPackageName() + "." + psiClass.getName();
            //检查这个class与xml中的namespace是否一致
            String namespace = rootTag.getAttributeValue("namespace");
            //校验不一致，跳过
            if (!packageName.equals(namespace)) {
                continue;
            }
            //获取接口跳转位置
            interfaceTarget = rootTag;
            //获取 mapper.xml 子节点
            XmlTag[] subTags = rootTag.getSubTags();
            //获取这个class中的所有方法
            PsiElement[] children = psiClass.getChildren();
            for (PsiElement child : children) {
                if (!(child instanceof PsiMethod)) {
                    continue;
                }
                //该方法与xml中的子节点匹配
                for (XmlTag subTag : subTags) {
                    String id = subTag.getAttributeValue("id");
                    //如果方法名与mapper.xml中的id一致，可以添加图标
                    if (((PsiMethod) child).getName().equals(id)) {
                        psiMethodXmlMap.put((PsiMethod) child, subTag);
                    }
                }
            }
        }
        //给接口添加按钮
        if (interfaceTarget != null && ((PsiClass) element).getNameIdentifier() != null) {
            NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(AllIcons.Nodes.Interface)
                            .setTarget(interfaceTarget)
                            .setAlignment(GutterIconRenderer.Alignment.RIGHT)
                            .setTooltipText("Go To " + interfaceTarget.getName());
            result.add(builder.createLineMarkerInfo(((PsiClass) element).getNameIdentifier()));
        }
        //给方法和xml添加按钮
        if (psiMethodXmlMap.size() > 0) {
            NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(AllIcons.Nodes.Interface)
                            .setTooltipText("Go To XML")
                            .setAlignment(GutterIconRenderer.Alignment.RIGHT);
            for (Map.Entry<PsiMethod, XmlTag> entry : psiMethodXmlMap.entrySet()) {
                if (entry.getKey().getNameIdentifier() != null) {
                    builder.setTarget(entry.getValue())
                           .setTooltipText("Go To " + entry.getValue().getName());
                    result.add(builder.createLineMarkerInfo(entry.getKey().getNameIdentifier()));
                }
            }
        }
    }

}
