
import java.util.ArrayList;
import java.util.List;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Nishen K Peiris
 */
public class Database {
    private static ArrayList<String> cardList;
    private static List<String> cardTypes;
    
    static void loadDatabase() {
        cardList = new ArrayList<String> ();
        cardTypes = new ArrayList<String>();
        
        cardTypes.add("club");
        cardTypes.add("diamond");
        cardTypes.add("heart");
        cardTypes.add("spade");
        //add all cards to the card list
        int i, j;
        for(i = 0; i < 4; i++) {
            for(j = 2; j < 15; j++) {
                String cardType = cardTypes.get(i);
                cardList.add(cardType + "/" + j + ".png");
            }
        }
        
        // shuffle cards
        shuffleCards();
    }

    private static void shuffleCards() {
        // traverse through the whole card list, swap every card
        // with a randomly choosen card from the same list
        for (int i = 0; i < cardList.size(); i++) {
            int swapIndex = (int) (Math.random() * 52);
            String temp = cardList.get(swapIndex);
            cardList.set(swapIndex, cardList.get(i));
            cardList.set(i, temp);
        }
    }

    public static ArrayList<String> getCardList() {
        return cardList;
    }
}
