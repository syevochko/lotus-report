package com.fuib.lotus.agents.report;

import com.fuib.lotus.LNEnvironment;
import com.fuib.lotus.utils.Tools;
import lotus.domino.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

/**
 * Created by sergey.yevochko on 5/11/2017.
 * Class attaches created report to the search result document in ENV_SEARCH database
 */
public class SearchDBReportBuilder extends SingleFileReportBuilder {

	public Document createSearchResultDoc(Database dbSearch, File fileCsv) throws NotesException {
        Document docSearchResult;
        RichTextItem rtItem;
        try {
            docSearchResult = dbSearch.createDocument();
            docSearchResult.replaceItemValue("Form", "SearchResult").recycle();

            rtItem = docSearchResult.createRichTextItem("fdBody");
            rtItem.embedObject(EmbeddedObject.EMBED_ATTACHMENT, "", fileCsv.getAbsolutePath(), "");
            rtItem.recycle();

            Item author = docSearchResult.replaceItemValue("$$Author", dbSearch.getParent().getUserName());
            author.setAuthors(true);
            author.recycle();

            Vector v = new Vector(Arrays.asList(new String[]{"[ViewAllResults]", dbSearch.getParent().getUserName()}));
            Item readers = docSearchResult.replaceItemValue("base_readers", v);
            readers.setReaders(true);
            readers.recycle();

            docSearchResult.replaceItemValue("fdCreated", dbSearch.getParent().createDateTime(new Date())).recycle();
            String title = "отчет по потоку МБ: Заявка на финансирование " + fileCsv.getName();
            docSearchResult.replaceItemValue("fdDescr", title).recycle();
            docSearchResult.replaceItemValue("fdRes", title).recycle();

            docSearchResult.save();

        } catch (NotesException ne) {
            ne.printStackTrace();
            throw new NotesException(LNEnvironment.ERR_CUSTOM, "Error while create doc in search DB to attach file.");
        }

        return docSearchResult;
    }

}
