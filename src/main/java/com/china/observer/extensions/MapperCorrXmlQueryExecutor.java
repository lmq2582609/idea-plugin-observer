package com.china.observer.extensions;

import com.china.observer.service.MapperCorrXmlService;
import com.china.observer.service.impl.MapperCorrXmlServiceImpl;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

/**
 * Mapper接口与Xml建立关系关联关系
 * 实现快捷跳转，如：ctrl+alt+鼠标左键、ctrl+B
 */
public class MapperCorrXmlQueryExecutor extends QueryExecutorBase<PsiElement, DefinitionsScopedSearch.SearchParameters> {

    public MapperCorrXmlQueryExecutor() {
        //标识该查询是只读的
        super(true);
    }

    @Override
    public void processQuery(DefinitionsScopedSearch.@NotNull SearchParameters queryParameters, @NotNull Processor<? super PsiElement> consumer) {
        PsiElement element = queryParameters.getElement();
        MapperCorrXmlService mapperCorrXml = new MapperCorrXmlServiceImpl();
        PsiElement xmlElement = mapperCorrXml.mapperCorrelationXml(element);
        if (xmlElement != null) {
            consumer.process(xmlElement);
        }
    }

}
