package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;


public final class UrlController implements CrudHandler {
    public static Handler updateUrl = ctx -> {
        String urlID = ctx.pathParam("url-id");

        if (!urlID.matches("\\d+")) {
            ctx.status(HttpStatus.BAD_REQUEST).result("String ID must be numeric");
            return;
        }

        Url url = new QUrl().id.eq(Integer.parseInt(urlID)).findOne();
        if (url == null) {
            ctx.status(HttpStatus.NOT_FOUND).result("Not Found");
            return;
        }

        ctx.attribute("url", url);
        ctx.render("update.html");
    };

    @Override
    public void create(@NotNull Context ctx) {
        String userInput = ctx.formParam("url");
        URL url = null;

        try {
            url = new URL(userInput);
        } catch (MalformedURLException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.sessionAttribute("flash", "Некорректный URL. Не забудьте указать \"http\" или \"https\"");
            ctx.attribute("url", userInput);
            ctx.render("index.html");
            ctx.consumeSessionAttribute("flash");
            return;
        }

        Url newUrl = getNormalisedUrl(url);
        Optional<Url> existingUrl = new QUrl().name.eq(newUrl.getName()).findOneOrEmpty();

        if (existingUrl.isPresent()) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.attribute("url", userInput);
            ctx.render("index.html");
            ctx.consumeSessionAttribute("flash");
            return;
        }

        newUrl.save();
        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.redirect("/urls");
    }

    @Override
    public void delete(@NotNull Context ctx, @NotNull String urlID) {

    }

    @Override
    public void getAll(@NotNull Context ctx) {
        final int urlsPerPage = 10;
        String pageQuery = ctx.queryParam("page");
        int pageNum = pageQuery == null ? 1 : Integer.parseInt(pageQuery);
        ctx.attribute("pageNum", pageNum);

        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow((pageNum - 1) * urlsPerPage)
                .setMaxRows(urlsPerPage)
                .findPagedList();
        List<Url> list = pagedUrls.getList();
        ctx.attribute("urls", list);
        ctx.render("urls.html");
        ctx.consumeSessionAttribute("flash");
    }

    @Override
    public void getOne(@NotNull Context ctx, @NotNull String urlID) {
        if (!urlID.matches("\\d+")) {
            ctx.status(HttpStatus.BAD_REQUEST).result("String ID must be numeric");
            return;
        }

        Url url = new QUrl().id.eq(Integer.parseInt(urlID)).findOne();
        if (url == null) {
            ctx.status(HttpStatus.NOT_FOUND).result("Not Found");
            return;
        }
        List<UrlCheck> checks = url.getUrlChecks();

        ctx.attribute("checks", checks);
        ctx.attribute("url", url);
        ctx.render("url.html");
        ctx.consumeSessionAttribute("flash");
    }

    @Override
    public void update(@NotNull Context ctx, @NotNull String urlID) {
        if (!urlID.matches("\\d+")) {
            ctx.status(HttpStatus.BAD_REQUEST).result("String ID must be numeric");
            return;
        }

        Url oldUrl = new QUrl().id.eq(Integer.parseInt(urlID)).findOne();
        if (oldUrl == null) {
            ctx.status(HttpStatus.NOT_FOUND).result("Not Found");
            return;
        }

        Url updatedUrl = ctx.bodyAsClass(Url.class);
        URL updatedRaw = null;

        try {
            updatedRaw = new URL(updatedUrl.getName());
        } catch (MalformedURLException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.sessionAttribute("flash", "Некорректный URL. Не забудьте указать \"http\" или \"https\"");
            ctx.attribute("url", updatedUrl.getName());
            ctx.render("update.html");
            ctx.consumeSessionAttribute("flash");
            return;
        }

        oldUrl.setName(getNormalisedUrl(updatedRaw).getName());
        oldUrl.save();

        ctx.sessionAttribute("flash", "Страница успешно обновлена");
        ctx.redirect("/urls");
    }

    @NotNull
    private static Url getNormalisedUrl(URL url) {
        String normalisedUrl = url.getProtocol() + "://" + url.getAuthority();

        if (normalisedUrl.startsWith("http://www.")
                || normalisedUrl.startsWith("https://www.")) {
            normalisedUrl = normalisedUrl.replace("www.", "");
        }

        return new Url(normalisedUrl);
    }
}
