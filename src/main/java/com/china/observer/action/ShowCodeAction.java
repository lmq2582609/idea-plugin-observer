package com.china.observer.action;

import com.china.observer.entity.MethodXmlBO;
import com.china.observer.service.PsiElementHandlerService;
import com.china.observer.service.impl.PsiElementHandlerServiceImpl;
import com.china.observer.util.AssertUtil;
import com.china.observer.util.PsiUtil;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.*;

public class ShowCodeAction extends AnAction {

    private Project project;

    private Editor editor;

    @Override
    public void actionPerformed(AnActionEvent e) {
        this.project = e.getProject();
        this.editor = e.getData(CommonDataKeys.EDITOR);
        if (Objects.isNull(project) || Objects.isNull(editor)) {
            return;
        }
        //通过项目的文档管理器，获取当前编辑器展示的文件
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (Objects.isNull(psiFile)) {
            return;
        }
        //获取展示的文件中，光标所在的PsiElement
        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (Objects.isNull(element)) {
            return;
        }
        //必须是 PsiIdentifier 才处理
        if (!(element instanceof PsiIdentifier)) {
            return;
        }
        //获取上一级，一般类型是 PsiReferenceExpression
        PsiElement pe = element.getParent();
        //悬浮在函数上 -> PsiReferenceExpression
        if (pe instanceof PsiReferenceExpression) {
            PsiReferenceExpression reference = (PsiReferenceExpression) pe;
            PsiElement resolve = reference.resolve();
            //方法 -> 需要判断接口还是实现类
            if (resolve instanceof PsiMethod) {
                //检查是否为get/set
                PsiElementHandlerService handlerService = new PsiElementHandlerServiceImpl();
                PsiField psiField = handlerService.checkMethodIsGetterOrSetter((PsiMethod) resolve);
                if (psiField != null) {
                    showCode(psiField);
                } else {
                    //其他函数
                    executePsiMethod(resolve);
                }
            } else if (resolve instanceof PsiField) {
                //当前类字段
                showCode(resolve);
            } else if (resolve instanceof PsiLocalVariable) {
                //局部变量
                showCode(resolve);
            }
        } else if (pe instanceof PsiField) {
            //悬浮在当前类的字段上 -> PsiField
            showCode(pe);
        }
    }

    /**
     * 执行method判定
     * @param pe
     */
    private void executePsiMethod(PsiElement pe) {
        //直接放到了函数名字上
        PsiMethod pm = (PsiMethod) pe;
        //需要判断是接口还是实现，接口的话可能有多个实现，实现的话直接展示。接口还需要判断是否为mybatis的mapper，如果是mapper展示对应的xml代码










        //检查PsiMethod是本地代码还是第三方jar包的代码，通过文件来识别
        VirtualFile virtualFile = pm.getContainingFile().getVirtualFile();
        if (virtualFile == null) {
            return;
        }
        //第三方代码文件路径
        String absolutePath = virtualFile.getPath();
        //本地项目路径
        String projectBasePath = project.getBasePath();
        // 第三方的代码 - 同样要检查接口还是实现类
        if (!absolutePath.startsWith(projectBasePath)) {
            XmlTag xmlTag = selectMethodRelationXml(pm);
            //不为空，说明是mybatis的mapper
            if (xmlTag != null) {
                showCode(xmlTag);
            } else {
                //其他第三方代码
                showCode(pm);
            }
        } else {
            //处理本地项目代码
            //方法所在的类
            PsiClass containingClass = pm.getContainingClass();
            AssertUtil.isNullNoException(containingClass);
            //接口
            if (containingClass.isInterface()) {
                //在整个项目中，查找获取到的方法(所有，包括实现类的方法)
                Collection<PsiMethod> methods = OverridingMethodsSearch.search(pm, GlobalSearchScope.projectScope(project), true)
                        .findAll();
                //如果方法有多个实现，展示列表，则进行选择
                if (methods.size() > 1) {
                    showImplList(methods, editor);
                } else if (methods.size() == 1) {
                    //只有 1 个实现，直接展示
                    showCode(methods.iterator().next());
                } else {
                    //插件内部错误
                    Messages.showMessageDialog("No corresponding reality found", "Notification", Messages.getInformationIcon());
                    return;
                }
            }
            //实现类代码，直接展示
            else {
                showCode(pm);
            }
        }
    }

    /**
     * 展示代码
     * @param psi
     */
    private <T extends PsiElement> void showCode(T psi) {
        EditorTextField editorTextField = PsiUtil.createEditorTextField(psi);
        JBScrollPane scrollPane = new JBScrollPane(editorTextField);
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scrollPane, editor.getComponent())
                .setRequestFocus(true)
                .setResizable(true)
                .setMovable(true)
                .setTitle("Show Code")
                .createPopup();
        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                if (editorTextField.getEditor() != null) {
                    EditorFactory.getInstance().releaseEditor(editorTextField.getEditor());
                }
            }
        });
        popup.showInFocusCenter();
    }

    /**
     * 展示实现类列表
     * @param methods
     * @param editor
     */
    private void showImplList(Collection<PsiMethod> methods, Editor editor) {
        List<PsiMethod> methodList = new ArrayList<>(methods);
        JBList<PsiMethod> list = new JBList<>(methodList);
        //默认选中第一个
        list.setSelectedIndex(0);
        //设置列表字体
        list.setFont(editor.getColorsScheme().getFont(EditorFontType.PLAIN));
        //设置列表展示形式 - PsiClassListCellRenderer为展示class的样式
        list.setCellRenderer(new ListCellRenderer<PsiMethod>() {
            private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
            @Override
            public Component getListCellRendererComponent(JList<? extends PsiMethod> jList, PsiMethod psiMethod, int index, boolean isSelected, boolean cellHasFocus) {
                // 根据value中的信息，生成用于展示的Component对象
                JLabel label = (JLabel) defaultRenderer.getListCellRendererComponent(list, psiMethod.getName(), index, isSelected, cellHasFocus);
                //设置图标
                label.setIcon(psiMethod.getContainingClass().getIcon(Iconable.ICON_FLAG_READ_STATUS));
                // 添加返回类型和参数信息
                label.setText(psiMethod.getContainingClass().getName() + " (" + ((PsiJavaFileImpl) psiMethod.getContainingFile()).getPackageName() + ")");
                return label;
            }
        });
        //鼠标点击事件
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //点击 1 次
                if (e.getClickCount() == 1) {
                    //展示对应方法的实现代码
                    PsiMethod pm = list.getSelectedValue();
                    if (pm != null) {
                        showCode(pm);
                    }
                }
            }
        });
        //鼠标悬停事件
        list.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                list.setSelectedIndex(index);
                list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        });
        JPanel content = new JPanel(new BorderLayout());
        content.add(list, BorderLayout.CENTER);
        JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(content, null)
                .setTitle("I Love You dong dong")
                .setResizable(true) // 设置弹窗可以调节大小
                .setMovable(true) // 设置弹窗可以拖动
                .setRequestFocus(true)
                .setFocusable(true)
                .createPopup();
        popup.showInBestPositionFor(editor);
    }

    /**
     * 获取方法关联的XML
     * @param pm
     * @return
     */
    private XmlTag selectMethodRelationXml(PsiMethod pm) {
        //检查是否为mapper
        PsiClass containingClass = pm.getContainingClass();
        //查找项目中所有XML文件
        Collection<VirtualFile> projectAllXML = FileTypeIndex.getFiles(XmlFileType.INSTANCE,
                GlobalSearchScope.projectScope(pm.getProject()));
        if (projectAllXML.size() == 0) {
            return null;
        }
        //本地项目路径 - 忽略路径
        String ideaPath = containingClass.getProject().getBasePath() + "/.idea";
        //查找对应的sql
        for (VirtualFile virtualFile : projectAllXML) {
            //忽略.idea目录，跳过
            if (virtualFile.getPath().startsWith(ideaPath)) {
                continue;
            }
            //检查是否是mapper，返回xml根节点
            XmlTag rootTag = MethodXmlBO.checkIsMapper(virtualFile, containingClass);
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
                    //匹配成功
                    return subTag;
                }
            }
        }
        return null;
    }
}
