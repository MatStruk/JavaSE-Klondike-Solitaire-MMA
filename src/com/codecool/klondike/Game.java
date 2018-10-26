package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.scene.control.Button;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        if(!card.isFaceDown()) {
            Pile activePile = card.getContainingPile();
            List<Card> listOfCards = card.getContainingPile().getCards();
            List<Card> draggedElements = new ArrayList<Card>();
            int j = -1;
            for (int i = 0; i < listOfCards.size(); i++) {
                if (listOfCards.get(i) == card) {
                    j = i;
                }
                if (j == i) {
                    draggedElements.add(listOfCards.get(j));
                }
                j++;
            }
            if (activePile.getPileType() == Pile.PileType.STOCK) {
                return;
            }
            double offsetX = e.getSceneX() - dragStartX;
            double offsetY = e.getSceneY() - dragStartY;


            draggedCards.clear();
            draggedCards = draggedElements;
            for (Card element : draggedElements) {
                element.getDropShadow().setRadius(20);
                element.getDropShadow().setOffsetX(10);
                element.getDropShadow().setOffsetY(10);

                element.toFront();
                element.setTranslateX(offsetX);
                element.setTranslateY(offsetY);
            }
         }
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        int numberOfDraggedCards = draggedCards.size();
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        Pile pile = getValidIntersectingPile(card, tableauPiles);
        if (pile == null) {
            pile = getValidIntersectingPile(card, foundationPiles);
        }

        if (pile != null && !(numberOfDraggedCards > 1 && pile.getPileType() == Pile.PileType.FOUNDATION)) {
            handleValidMove(card, pile);
            if(card.getContainingPile().getPileType() != Pile.PileType.DISCARD && (card.getContainingPile().getCards().size() > 1 && card.getContainingPile().getCards().get(card.getContainingPile().getCards().size() - numberOfDraggedCards -1).isFaceDown())) {
                card.getContainingPile().getCards().get(card.getContainingPile().getCards().size() - numberOfDraggedCards -1).flip();
            }
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }
        if (isGameWon()) {
            showModalMessage("Congratulations, you won!");
        }
    };

    public boolean isGameWon() {
        int foundationCards = 0;
        for (Pile foundation : foundationPiles) {
            foundationCards += foundation.numOfCards();
        }
        System.out.println(foundationCards);
        if (foundationCards == 51) {
            return true;
        }
        return false;
    }

    public Game() {
        createnewGame();
    }

    public void createnewGame() {
        deck = Card.createNewDeck();
        shuffleDeck(deck);
        initPiles();
        dealCards();
        restartButton();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        stockPile.clear();
        Collections.reverse(discardPile.getCards());
        for (Card card : discardPile.getCards()) {
            if (!card.isFaceDown()) {
                card.flip();
            }
            stockPile.addCard(card);
        }
        discardPile.clear();
        System.out.println("Stock refilled from discard pile.");
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        if (destPile.getPileType().equals(Pile.PileType.FOUNDATION)) {
            if (destPile.getTopCard() == null) {
                if (destPile.isEmpty() && card.getRank() == 1) {
                    return true;
                }
            } else {
                if (destPile.getTopCard().getSuit() == card.getSuit() && destPile.getTopCard().getRank() == card.getRank() - 1) {
                    return true;
                }
            }
        } else if (destPile.getPileType().equals(Pile.PileType.TABLEAU)) {
            if (destPile.getTopCard() == null) {
                if (destPile.isEmpty() && card.getRank() == 13) {
                    return true;
                }
            } else {
                if (destPile.getTopCard().isRed() != card.isRed() && destPile.getTopCard().getRank() == card.getRank() + 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile)) {
                result = pile;
            }
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void dealCards() {
        Iterator<Card> deckIterator = deck.iterator();
        int i;
        int j = 0;
        for (Pile pile : tableauPiles) {
            pile.clear();
            j++;
            i = j;
            while (i > 0) {
                Card cardToAdd = deckIterator.next();
                pile.addCard(cardToAdd);
                addMouseEventHandlers(cardToAdd);
                getChildren().add(cardToAdd);
                i--;
            }
            if (pile.getTopCard().isFaceDown()) {
                pile.getTopCard().flip();
            }
        }
        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });

    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

    public void shuffleDeck(List deck) {
        Collections.shuffle(deck);
    }

    public void restartButton() {
        Button restartButton = new Button();
        restartButton.setText("Restart");
        restartButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                restartGame();
            }
        });
        getChildren().add(restartButton);
    }

    public void restartGame() {
        stockPile.clear();
        discardPile.clear();
        tableauPiles.clear();
        foundationPiles.clear();
        this.getChildren().clear();
        createnewGame();
    }

    public void callReload(String pathToImage) {
        for (Card card : deck) {
            card.reloadCardImages(pathToImage);
        }
    }
    private void showModalMessage(String message) {
        final Stage dialog = new Stage();
        Text text = new Text(message);
        Scene dialogScene;
        dialog.initModality(Modality.APPLICATION_MODAL);
        VBox dialogVbox = new VBox(20);
        text.setStyle("-fx-font: 20 arial;");
        dialogVbox.getChildren().add(text);
        dialogScene = new Scene(dialogVbox, 350, 50);
        dialog.setScene(dialogScene);
        dialog.show();
    }
}