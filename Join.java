/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 *
 * @author Nishen K Peiris
 */
public class Join extends HttpServlet {
    private final int PLAYERS = 4;
    
    private int noOfPlayers; //no. of players currently connected for the game
    private int toPlay; //whom to play next
    private String[] cardsStr; //cards played by players in the current trick
    private int[] score; //scores of the current round
    private String trumpSuit; //the trup suit of the current round
    private int trickLeader; //position of the player, who leads the trick
    private int winner; //position of the winner

    @Override
    public void init() throws ServletException {
        noOfPlayers = 0;
        toPlay = 1;
        cardsStr = new String[4];
        score = new int[4];
        trickLeader = 1;
        
        Database.loadDatabase();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        String playedCard = req.getParameter("card").replace("cards/", "");
        //check if the player is the one to play
        if(session.getAttribute("status").equals(Status.Play)) {
            if(isCardAvailable(playedCard, (List<String>) session.getAttribute("cards"))) {
                if((int) (Integer)session.getAttribute("position") != 1) {
                    if(isCardValid(playedCard, (List<String>) session.getAttribute("cards"))) {
                        markCardAsPlayed(playedCard, session);
                        //increment toPlay, to give the chance to the next player
                        toPlay = toPlay % PLAYERS + 1;
                    }
                } else {
                    markCardAsPlayed(playedCard, session);
                    //increment toPlay, to give the chance to the next player
                    toPlay = toPlay % PLAYERS + 1;
                }
            } else {
                //played a card, which isn't available
            }
        
            //if the player is the last player (with position 4), he should update the score board
            if((int) (Integer) session.getAttribute("position") == 4) {
                updateScore();
            }
        } else {
            //player can't play because this is not the player to play
        }
    }


    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        
        if(session.isNew()) {
            session.setAttribute("position", ++noOfPlayers);
            session.setAttribute("status", Status.Loading);
        }

        if(noOfPlayers == PLAYERS) {
            if(session.getAttribute("status").equals(Status.Loading)) {        
                //distribute cards to the player
                dealCards(session);
            }

            if((int)(Integer)session.getAttribute("position") == toPlay) {
                session.setAttribute("status", Status.Play);
            } else {
                session.setAttribute("status", Status.Waiting);
            }
            
            if(session.getAttribute("status").equals(Status.Play)) {
                //get card list from session object, convert to json format and send to the player
                List<String> cards = (List<String>) session.getAttribute("cards");
                JSONArray cardArray = new JSONArray();
                for (String card : cards) {   
                    JSONObject tempCard = new JSONObject();
                    tempCard.put("image", "cards/" + card);
                    cardArray.put(tempCard);
                }
                JSONObject json = new JSONObject();
                response.setContentType("text/event-stream");
                response.setCharacterEncoding("UTF-8");   
                PrintWriter out = response.getWriter();
                json.put("trumpSuit", trumpSuit);
                json.put("cards", cardArray);
                //set cards played by other players
                for(int i = 1; i < (int) (Integer) session.getAttribute("position"); i++) {
                    json.put("card" + i, "cards/" + cardsStr[i-1]);
                }
                json.put("showHand", true);
                json.put("showCards", true);
                json.put("message", "Play your card");
                out.write("data: " + json + "\n\n");
                out.close();
            } else if(session.getAttribute("status").equals(Status.Waiting)) {
                //get card list from session object, convert to json format and send to the player
                List<String> cards = (List<String>) session.getAttribute("cards");
                JSONArray cardArray = new JSONArray();
                for (String card : cards) {   
                    JSONObject tempCard = new JSONObject();
                    tempCard.put("image", "cards/" + card);
                    cardArray.put(tempCard);
                }
                JSONObject json = new JSONObject();
                response.setContentType("text/event-stream");
                response.setCharacterEncoding("UTF-8");   
                PrintWriter out = response.getWriter();
                json.put("trumpSuit", trumpSuit);
                json.put("cards", cardArray);
                //set cards played by other players
                int j = 1;
                for(int i = 1; i <= 4; i++) {
                    if(cardsStr[i-1] != null && i != (int) (Integer) session.getAttribute("position")) {
                        json.put("card" + j++, "cards/" + cardsStr[i-1]);
                    }
                }
                //set mycard if the player has
                if(cardsStr[(int) (Integer) session.getAttribute("position") - 1] != null) {
                    json.put("mycard", "cards/" + cardsStr[(int) (Integer) session.getAttribute("position") - 1]);
                }
                json.put("showHand", true);
                json.put("showCards", true);
                json.put("message", "Wait for others to play");
                out.write("data: " + json + "\n\n");
                out.close(); 
            } else {
                //undefined state
            }
        } else {
            JSONObject json = new JSONObject();
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");   
            PrintWriter out = response.getWriter();
            json.put("cards", new JSONArray());
            json.put("showHand", false);
            json.put("showCards", false);
            if(noOfPlayers == 1) {
                json.put("message", "Waiting for others to connect. Only 1 player connected ..");
            } else {
                json.put("message", "Waiting for others to connect. Only " + noOfPlayers +  " players connected ..");
            }
            out.write("data: " + json + "\n\n");
            out.close();
        } 
    }

    private void dealCards(HttpSession session) {      
        List<String> cards = new ArrayList();
        ArrayList<String> cardList = Database.getCardList();
        int start = 13*((int) (Integer) session.getAttribute("position")-1);
        int end = start + 12;
        
        for(int i = start; i <= end; i++) {
            cards.add(cardList.get(i));
        }
        
        //if the player is the last(with position 4), he should set the trump suit
        if((int) (Integer) session.getAttribute("position") == 4) {
            trumpSuit = (cards.get(12).split("/"))[0];
        }
        
        //add card list of the player to the session object
        session.setAttribute("cards", cards);
    }

    private boolean isCardAvailable(String playedCard, List<String> cards) {
        for (String tempCard : cards) {   
            if(tempCard.equals(playedCard)) {
                return true;
            }
        }
        return false;
    }

    private void markCardAsPlayed(String playedCard, HttpSession session) {
        List<String> list = (List<String>) session.getAttribute("cards");
        //remove the played card from the card list, in session object
        for (Iterator<String> iter = list.listIterator(); iter.hasNext(); ) {
            String card = iter.next();
            if (card.equals(playedCard)) {
                iter.remove();
            }
        }
        
        //set played card in cardsStr
        cardsStr[(int) (Integer) session.getAttribute("position") - 1] = playedCard;
        
        //go to waiting state
        session.setAttribute("status", Status.Waiting);
    }

    private void updateScore() {
        int winnerIndex = trickLeader; //position of the winner
        String leaderCardType = cardsStr[trickLeader-1].split("/")[0];
        int leaderCardValue = Integer.parseInt((cardsStr[trickLeader-1].split("/")[1]).replace(".png", ""));
        boolean trumpPlayed = false;
        int trumpPlayedValue = 0;
        for(int i = 0; i < 4; i++) {System.out.println(i+"...");
            String currentCardtype = cardsStr[i].split("/")[0];
            int currentCardValue = Integer.parseInt(cardsStr[i].split("/")[1].replace(".png", ""));
            if(currentCardtype.equals(trumpSuit)) {
                if(trumpPlayed) {
                    if(trumpPlayedValue < currentCardValue) {
                        trumpPlayedValue = currentCardValue;
                        winnerIndex = i+1;
                    }
                } else {
                    trumpPlayed = true;
                    trumpPlayedValue = currentCardValue;
                    winnerIndex = i+1; 
                }
            } else if(currentCardtype.equals(leaderCardType) && trumpPlayed == false) {
                if(leaderCardValue < currentCardValue) {
                    winnerIndex = i+1;
                }
            }
        }
        score[winnerIndex]++;//finally, this is the leader, and get 1 point
        //winner should be the next to play
        System.out.println(winnerIndex);
    }

    private void findWinner() {
        int largest = score[0], index = 0;
        for (int i = 1; i < score.length; i++) {
          if ( score[i] > largest ) {
              largest = score[i];
              index = i;
           }
        }
        winner = 1+index;
    }

    private boolean isCardValid(String playedCard, List<String> cards) {
        String playedCardType = playedCard.split("/")[0];
        String trickLeaderCardType = (cardsStr[trickLeader-1].split("/"))[0];
        System.out.println(playedCardType + " " + trickLeaderCardType);
        if(!playedCardType.equals(trickLeaderCardType)) {
            // if the player doesn't have a card of the trick leading suit, then this play is valid.
            // otherwise invalid.
            for (Iterator<String> iter = cards.listIterator(); iter.hasNext(); ) {
                String cardType = (iter.next().split("/"))[0];
                if(cardType.equals(trickLeaderCardType)) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }
}