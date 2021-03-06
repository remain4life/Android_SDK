package ru.usedesk.sdk.internal.domain.interactor.knowledgebase;

import android.support.annotation.NonNull;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import ru.usedesk.sdk.external.entity.knowledgebase.ArticleBody;
import ru.usedesk.sdk.external.entity.knowledgebase.ArticleInfo;
import ru.usedesk.sdk.external.entity.knowledgebase.Category;
import ru.usedesk.sdk.external.entity.knowledgebase.KnowledgeBaseConfiguration;
import ru.usedesk.sdk.external.entity.knowledgebase.SearchQuery;
import ru.usedesk.sdk.external.entity.knowledgebase.Section;

public interface IKnowledgeBaseInteractor {

    @NonNull
    Single<List<Section>> getSectionsSingle();

    @NonNull
    Single<ArticleBody> getArticleSingle(long articleId);

    @NonNull
    Single<List<ArticleBody>> getArticlesSingle(@NonNull String searchQuery);

    @NonNull
    Single<List<ArticleBody>> getArticlesSingle(@NonNull SearchQuery searchQuery);

    @NonNull
    Single<List<Category>> getCategoriesSingle(long sectionId);

    @NonNull
    Single<List<ArticleInfo>> getArticlesSingle(long categoryId);

    void setConfiguration(@NonNull KnowledgeBaseConfiguration configuration);

    @NonNull
    Completable addViewsCompletable(long articleId);
}
