package com.svenruppert.urlshortener.ui.vaadin.views;



import com.svenruppert.urlshortener.ui.vaadin.MainLayout;
import com.svenruppert.urlshortener.ui.vaadin.security.AppRole;
import com.svenruppert.urlshortener.ui.vaadin.security.VisibleFor;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = YoutubeView.PATH, layout = MainLayout.class)
@VisibleFor(AppRole.USER)
public class YoutubeView
    extends VerticalLayout {

  public static final String PATH = "youtube";

  public YoutubeView() {
    IFrame youtubePlayer = new IFrame("https://www.youtube.com/embed/CxCMIc5Bx18");
    youtubePlayer.setWidth("800px");
    youtubePlayer.setHeight("450px");
    youtubePlayer.getElement().setAttribute("allowfullscreen", true);
    youtubePlayer.getElement().setAttribute("frameborder", "0");

    add(youtubePlayer);
    setSizeFull();
    setJustifyContentMode(JustifyContentMode.CENTER);
    setAlignItems(Alignment.CENTER);
  }
}