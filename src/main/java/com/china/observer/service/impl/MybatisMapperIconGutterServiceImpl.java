package com.china.observer.service.impl;

import com.china.observer.entity.MethodXmlBO;
import com.china.observer.service.MybatisIconGutterService;
import com.china.observer.service.PsiElementHandlerService;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlTag;
import java.util.*;
import java.util.stream.Collectors;


/**
 * mybatis mapper图标处理
 */
public class MybatisMapperIconGutterServiceImpl implements MybatisIconGutterService {

    @Override
    public List<RelatedItemLineMarkerInfo<PsiElement>> gutterIconHandler(PsiElement element) {
        //如果不是接口，跳过
        if (!(element instanceof PsiClass) || !((PsiClass) element).isInterface()) {
            return Collections.emptyList();
        }
        PsiClass psiClass = (PsiClass) element;
        //将当前class与xml进行匹配
        PsiManager psiManager = PsiManager.getInstance(psiClass.getProject());
        //查找项目中所有XML文件
        Collection<VirtualFile> projectAllXML = FileTypeIndex.getFiles(XmlFileType.INSTANCE,
                GlobalSearchScope.projectScope(psiClass.getProject()));
        //未查找到项目中xml文件，跳过
        if (projectAllXML.size() == 0) {
            return Collections.emptyList();
        }
        PsiElementHandlerService handlerService = new PsiElementHandlerServiceImpl();
        List<MethodXmlBO> methodXmlBOList = new ArrayList<>();
        projectAllXML.forEach(virtualFile -> {
            //检查是否是mapper，返回xml根节点
            XmlTag rootTag = handlerService.checkIsMapper(virtualFile, psiClass);
            if (rootTag == null) {
                return;
            }
            //接口与xml根节点跳转位置已确定
            MethodXmlBO entity = MethodXmlBO.buildMethodXmlEntity(psiClass, rootTag);
            methodXmlBOList.add(entity);
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
                        MethodXmlBO methodXmlBO = MethodXmlBO.buildMethodXmlEntity(child, subTag);
                        methodXmlBOList.add(methodXmlBO);
                    }
                }
            }
        });
        //创建图标
        if (methodXmlBOList.size() > 0) {
            NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
                            .setAlignment(GutterIconRenderer.Alignment.RIGHT);
            return methodXmlBOList
                    .stream()
                    .map(b -> {
                        //目标 -> xml节点
                        builder.setTarget(b.getXml());
                        //图标文字
                        builder.setTooltipText("Go To " + b.getXml().getName());
                        //创建
                        PsiElement nameIdentifier = ((PsiNameIdentifierOwner) b.getElement()).getNameIdentifier();
                        if (nameIdentifier != null) {
                            return builder.createLineMarkerInfo(nameIdentifier);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
