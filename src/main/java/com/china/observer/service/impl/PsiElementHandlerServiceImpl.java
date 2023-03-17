package com.china.observer.service.impl;

import com.china.observer.entity.MethodXmlBO;
import com.china.observer.service.PsiElementHandlerService;
import com.china.observer.util.PsiUtil;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagValue;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class PsiElementHandlerServiceImpl implements PsiElementHandlerService {

    /**
     * 检查是否为get或set方法
     * @param method
     * @return
     */
    @Override
    public PsiField checkMethodIsGetterOrSetter(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return null;
        }
        String methodName = method.getName();

        if (methodName.startsWith("get")) {
            String fieldName = methodName.substring(3);
            PsiType returnType = method.getReturnType();
            if (Objects.isNull(returnType)) {
                return null;
            }
            //get方法返回结果的类型与字段类型一致
            PsiField psiField = selectClassField(containingClass, fieldName);
            return Optional.ofNullable(psiField)
                    .filter(field -> field.getType().getCanonicalText().equals(returnType.getCanonicalText()))
                    .orElse(null);
        }
        if (methodName.startsWith("set")) {
            //检查参数
            PsiParameter[] parameters = method.getParameterList().getParameters();
            if (parameters.length != 1) {
                return null;
            }
            String fieldName = methodName.substring(3);
            PsiParameter parameter = parameters[0];
            //set方法的参数类型与字段类型一致
            PsiField psiField = selectClassField(containingClass, fieldName);
            return Optional.ofNullable(psiField)
                    .filter(field -> field.getType().getCanonicalText().equals(parameter.getType().getCanonicalText()))
                    .orElse(null);
        }
        return null;
    }

    /**
     * 检查方法与xml关联
     * @param pm
     * @return
     */
    @Override
    public XmlTag selectMethodRelationXml(PsiMethod pm) {
        //检查是否为mapper
        PsiClass containingClass = pm.getContainingClass();
        //查找项目中所有XML文件
        Collection<VirtualFile> projectAllXML = FileTypeIndex.getFiles(XmlFileType.INSTANCE,
                GlobalSearchScope.projectScope(pm.getProject()));
        if (projectAllXML.size() == 0) {
            return null;
        }
        //本地项目路径 - 忽略路径
        String ideaPath = pm.getProject().getBasePath() + "/.idea";
        //查找对应的sql
        for (VirtualFile virtualFile : projectAllXML) {
            //忽略.idea目录，跳过
            if (virtualFile.getPath().startsWith(ideaPath)) {
                continue;
            }
            //检查是否是mapper，返回xml根节点
            XmlTag rootTag = checkIsMapper(virtualFile, containingClass);
            if (rootTag == null) {
                continue;
            }
            //此处说明，这个接口是一个mapper，继续查找对应的sql
            //获取 mapper.xml 子节点
            XmlTag[] subTags = rootTag.getSubTags();
            //方法与xml中的子节点匹配
            for (XmlTag subTag : subTags) {
                String id = subTag.getAttributeValue("id");
                //如果方法名与mapper.xml中的id一致，可以添加图标
                if (pm.getName().equals(id)) {
                    //如果是select查询
                    if ("select".equals(subTag.getName())) {
                        //解析select内部的其他节点
                        return executeXmlTag(pm, subTags, subTag);
                    } else {
                        //是其他节点，则返回
                        return subTag;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 检查是否为mapper，是的话返回这个节点
     * @param virtualFile
     * @param containingClass
     * @return
     */
    @Override
    public XmlTag checkIsMapper(VirtualFile virtualFile, PsiClass containingClass) {
        //如果不是一个接口，跳过
        if (containingClass == null || !containingClass.isInterface()) {
            return null;
        }
        PsiManager psiManager = PsiManager.getInstance(containingClass.getProject());
        PsiFile file = psiManager.findFile(virtualFile);
        if (!(file instanceof XmlFile)) {
            return null;
        }
        XmlFile xmlFile = (XmlFile) file;
        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag == null || !"mapper".equals(rootTag.getName())){
            return null;
        }
        //包路径
        String packageName = PsiUtil.getPackageName(containingClass) + "." + containingClass.getName();
        //检查这个class与xml中的namespace是否一致
        String namespace = rootTag.getAttributeValue("namespace");
        //校验一致，返回xml根节点
        if (packageName.equals(namespace)) {
            return rootTag;
        }
        return null;
    }

    /**
     * 查找类中的字段
     * @param psiClass
     * @param fieldName
     * @return
     */
    private PsiField selectClassField(PsiClass psiClass, String fieldName) {
        //首字母转小写
        String field = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
        //先使用findFiledByName查找一遍
        PsiField psiField = psiClass.findFieldByName(field, true);
        if (psiField != null) {
            return psiField;
        }
        //未查找到，再遍历查找一遍，但此处不能查找父类字段
        PsiField[] fields = psiClass.getFields();
        fieldName = fieldName.toLowerCase();
        for (PsiField f : fields) {
            if (f.getName().toLowerCase().equals(fieldName)) {
                return f;
            }
        }
        return null;
    }

    private XmlTag executeXmlTag(PsiMethod pm, XmlTag[] allTags, XmlTag selectTag) {
        //解析子节点里面的include节点
        XmlTag[] subTags = selectTag.getSubTags();
        if (subTags.length == 0) {
            return selectTag;
        }
        //新的xmlTag
        XmlTag tagFromText = createXmlTag(selectTag.getText(), pm.getProject());
        XmlTag[] selectSubTags = tagFromText.getSubTags();
        //查找select的子节点
        for (XmlTag selectSubTag : selectSubTags) {
            if ("include".equals(selectSubTag.getName())) {
                String refId = selectSubTag.getAttributeValue("refid");
                if (refId != null) {
                    for (XmlTag subTag2 : allTags) {
                        String id2 = subTag2.getAttributeValue("id");
                        if (refId.equals(id2)) {
                            //替换include节点
                            selectSubTag.replace(createXmlTag(subTag2.getText(), pm.getProject()));
                        }
                    }
                }
            }
        }
        return tagFromText;
    }

    /**
     * 创建新的XmlTag
     * @param code
     * @param project
     * @return
     */
    private XmlTag createXmlTag(String code, Project project) {
        XmlElementFactory elementFactory = XmlElementFactory.getInstance(project);
        return elementFactory.createTagFromText(code);
    }

}
