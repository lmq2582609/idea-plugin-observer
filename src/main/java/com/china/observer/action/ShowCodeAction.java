package com.china.observer.action;

import com.china.observer.util.AssertUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.EditorMouseAdapter;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.EditorMarkupModelImpl;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.popup.BalloonPopupBuilderImpl;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class ShowCodeAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        //获取项目
        Project project = e.getProject();
        AssertUtil.isNullNoException(project);
        //获取编辑器
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        AssertUtil.isNullNoException(editor);
        //通过项目的文档管理器，获取当前编辑器展示的文件
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        AssertUtil.isNullNoException(psiFile);
        //获取展示的文件中，光标所在的PsiElement
        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        AssertUtil.isNullNoException(element);
        //必须是 PsiIdentifier 才处理
        if (! (element instanceof PsiIdentifier)) {
            //提示注意光标位置
            Messages.showMessageDialog("Plugin internal error.", "Notification", Messages.getInformationIcon());
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
                PsiField psiField = checkGetterOrSetter(resolve);
                if (psiField != null) {
                    //展示字段信息
                    showCode(psiField, editor);
                } else {
                    //其他函数，直接展示
                    executePsiMethod(resolve, project, editor);
                }
            }
            //当前类字段
            else if (resolve instanceof PsiField) {

            }
            //局部变量
            else if (resolve instanceof PsiLocalVariable) {

            } else {
                //插件内部错误
                Messages.showMessageDialog("Plugin internal error", "Notification", Messages.getInformationIcon());
                return;
            }
        }
        //悬浮在类上 -> PsiJavaCodeReferenceElement
        else if (pe instanceof PsiJavaCodeReferenceElement) {



        }
        //悬浮在当前类的字段上 -> PsiField
        else if (pe instanceof PsiField) {



        }
        //悬浮在当前函数的参数上 -> PsiParameter
        else if (pe instanceof PsiParameter) {



        }
        //悬浮在函数的名字上 -> PsiMethod
        else if (pe instanceof PsiMethod) {
            executePsiMethod(pe, project, editor);
        } else {
            //插件内部错误
            Messages.showMessageDialog("Plugin internal error", "Notification", Messages.getInformationIcon());
            return;
        }

    }

    /**
     * 执行method判定
     * @param pe
     * @param project
     * @param editor
     */
    private void executePsiMethod(PsiElement pe, Project project, Editor editor) {
        //直接放到了函数名字上，需要判断是接口还是实现，接口的话可能有多个实现，实现的话直接展示
        PsiMethod pm = (PsiMethod) pe;
        //检查PsiMethod是本地代码还是第三方jar包的代码，通过文件来识别
        VirtualFile virtualFile = pm.getContainingFile().getVirtualFile();
        if (virtualFile != null) {
            //第三方代码文件路径
            String absolutePath = virtualFile.getPath();
            //本地项目路径
            String projectBasePath = project.getBasePath();
            // 第三方的代码 - 同样要检查接口还是实现类
            if (!absolutePath.startsWith(projectBasePath)) {
                //检查是否为mapper
                PsiClass containingClass = pm.getContainingClass();
                AssertUtil.isNullNoException(containingClass);





                //查找项目中所有XML文件
                Collection<VirtualFile> projectAllXML = FileTypeIndex.getFiles(XmlFileType.INSTANCE,
                        GlobalSearchScope.projectScope(project));
                //未查找到xml文件，跳过
                if (projectAllXML.size() == 0) {
                    return;
                }

                System.out.println();
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
                        showCode(methods.iterator().next(), editor);
                    } else {
                        //插件内部错误
                        Messages.showMessageDialog("No corresponding reality found", "Notification", Messages.getInformationIcon());
                        return;
                    }
                }
                //实现类代码，直接展示
                else {
                    showCode(pm, editor);
                }
            }
        } else {
            Messages.showMessageDialog("No corresponding reality found", "Notification", Messages.getInformationIcon());
            return;
        }
    }

    /**
     * 展示代码
     * @param psi
     * @param editor
     */
    private <T extends PsiElement> void showCode(T psi, Editor editor) {
        EditorTextField editorTextField = createEditorTextField(psi);
        JBScrollPane scrollPane = new JBScrollPane(editorTextField);
        // 创建一个JBPopup
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
                //释放资源
                if (editorTextField.getEditor() != null) {
                    System.out.println("释放资源");
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
                        showCode(pm, editor);
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
     * 格式化代码
     * @param code
     * @return
     */
    private String formatCode(String code) {
        //处理文本-格式化代码
        String[] split = code.split("\\n");
        StringJoiner sj = new StringJoiner("\n");
        for (String s : split) {
            //如果第1个字符是空格，则删除4个空格
            if (Character.isWhitespace(s.charAt(0))) {
                sj.add(s.substring(4));
            } else {
                sj.add(s);
            }
        }
        return sj.toString();
    }

    /**
     * 创建代码编辑器
     * @param psi
     * @return
     */
    private <T extends PsiElement> EditorTextField createEditorTextField(T psi) {
        //格式化代码
        String code = formatCode(psi.getText());
        //只读
        Document document = EditorFactory.getInstance().createDocument(code);
        document.setReadOnly(true);
        PsiFile psiFile = psi.getContainingFile();
        Language language = psiFile.getLanguage();
        EditorTextField editorTextField = new EditorTextField(document, psi.getProject(), language.getAssociatedFileType());
        //获取默认样式
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        editorTextField.setFont(scheme.getFont(EditorFontType.PLAIN));
        editorTextField.setOneLineMode(false);
        return editorTextField;
    }

    /**
     * 检查是否为get/set方法，如果是则返回该字段，如果不是则返回null
     * @param element
     * @return
     */
    private PsiField checkGetterOrSetter(PsiElement element) {
        PsiMethod method = (PsiMethod) element;
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return null;
        }
        String methodName = method.getName();
        if (methodName.startsWith("get")) {
            String fieldName = methodName.substring(3);
            PsiType returnType = method.getReturnType();
            //get方法返回结果的类型与字段类型一致
            PsiField psiField = selectClassField(containingClass, fieldName);
            return Optional.ofNullable(psiField)
                    .filter(field -> field.getType().getCanonicalText().equals(returnType.getCanonicalText()))
                    .orElse(null);
        }
        //set方法必须有1个参数
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
}
