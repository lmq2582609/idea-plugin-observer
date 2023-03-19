package com.china.observer.action;

import com.china.observer.service.PsiElementHandlerService;
import com.china.observer.service.impl.PsiElementHandlerServiceImpl;
import com.china.observer.util.MybatisUtil;
import com.china.observer.util.PsiElementUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.components.JBList;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ShowCodeAction extends AnAction {

    private Project project;

    private Editor editor;

    @Override
    public void actionPerformed(AnActionEvent e) {
        this.project = e.getProject();
        this.editor = e.getData(CommonDataKeys.EDITOR);
        if (this.project == null || this.editor == null) {
            return;
        }
        //获取光标处的psiElement
        PsiElement psiElement = PsiElementUtil.findPsiElementByDocument(this.project, this.editor);
        //必须是 PsiIdentifier 才处理
        if (!(psiElement instanceof PsiIdentifier)) {
            return;
        }
        //获取上一级，一般类型是 PsiReferenceExpression
        PsiElement pe = psiElement.getParent();
        //悬浮在函数上 -> PsiReferenceExpression
        if (pe instanceof PsiReferenceExpression) {
            PsiReferenceExpression reference = (PsiReferenceExpression) pe;
            PsiElement resolve = reference.resolve();
            //方法 -> 需要判断接口还是实现类
            if (resolve instanceof PsiMethod) {
                PsiMethod psiMethod = (PsiMethod) resolve;
                PsiClass psiClass = psiMethod.getContainingClass();
                if (Objects.isNull(psiClass)) {
                    return;
                }
                //检查是否为get/set方法
                PsiElementHandlerService handlerService = new PsiElementHandlerServiceImpl();
                PsiField psiField = handlerService.checkMethodIsGetterOrSetter(psiMethod);
                //是get/set方法，则展示字段信息
                if (psiField != null) {
                    PsiElementUtil.showCode(psiField, this.editor);
                } else {
                    //其他函数的判断，首先判断是本地项目代码还是第三方依赖的代码
                    boolean localPsiClass = PsiElementUtil.isLocalPsiClass(psiClass);
                    if (localPsiClass) {
                        //本地代码
                        localCodeHandler(psiClass, psiMethod);
                    } else {
                        //其他第三方代码，直接展示
                        PsiElementUtil.showCode(psiMethod, this.editor);
                    }
                }
            } else if (resolve instanceof PsiField) {
                PsiElementUtil.showCode(resolve, this.editor);
            } else if (resolve instanceof PsiLocalVariable) {
                //局部变量
                PsiElementUtil.showCode(resolve, this.editor);
            }
        } else if (pe instanceof PsiField) {
            //悬浮在当前类的字段上 -> PsiField
            PsiElementUtil.showCode(pe, this.editor);
        }
    }

    /**
     * 处理本地代码
     * @param psiClass
     * @param psiMethod
     */
    private void localCodeHandler(PsiClass psiClass, PsiMethod psiMethod) {
        //接口
        if (psiClass.isInterface()) {
            //在整个项目中，查找获取到的方法(所有，包括实现类的方法)
            Collection<PsiMethod> methods = OverridingMethodsSearch.search(psiMethod,
                            GlobalSearchScope.projectScope(this.project), true).findAll();
            //如果方法有多个实现，展示列表，进行选择
            if (methods.size() > 1) {
                showImplList(methods);
            } else if (methods.size() == 1) {
                //只有 1 个实现，直接展示
                PsiElementUtil.showCode(methods.iterator().next(), this.editor);
            } else {
                //接口没有实现类，可能是mybatis的mapper，检查是否为mybatis的mapper
                XmlTag xmlTag = MybatisUtil.findXmlByPsiMethod(psiMethod);
                if (xmlTag != null) {
                    //解析<include />节点
                    XmlTag newXmlTag = MybatisUtil.executeXmlTag(xmlTag, this.project);
                    PsiElementUtil.showCode(newXmlTag, this.editor);
                }
            }
        } else {
            //实现类代码，直接展示
            PsiElementUtil.showCode(psiMethod, this.editor);
        }
    }

    /**
     * 展示实现类列表
     * @param methods
     */
    private void showImplList(Collection<PsiMethod> methods) {
        List<PsiMethod> methodList = new ArrayList<>(methods);
        JBList<PsiMethod> list = new JBList<>(methodList);
        //默认选中第一个
        list.setSelectedIndex(0);
        //设置列表字体
        list.setFont(this.editor.getColorsScheme().getFont(EditorFontType.PLAIN));
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
        //鼠标点击事件，点击展示实现类代码
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //点击 1 次
                if (e.getClickCount() == 1) {
                    //展示对应方法的实现代码
                    PsiMethod pm = list.getSelectedValue();
                    if (pm != null) {
                        PsiElementUtil.showCode(pm, editor);
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
        JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(content, editor.getComponent())
                .setTitle("Show Implements")
                .setResizable(true) // 设置弹窗可以调节大小
                .setMovable(true) // 设置弹窗可以拖动
                .setRequestFocus(true)
                .setFocusable(true)
                .createPopup();
        popup.showInBestPositionFor(editor);
    }

}
