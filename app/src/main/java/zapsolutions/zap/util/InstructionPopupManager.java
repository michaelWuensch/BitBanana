package zapsolutions.zap.util;

import java.util.ArrayList;

public class InstructionPopupManager {

    public static String POPUP_ID_FIRST_DEPOSIT_1 = "firstDeposit1";
    public static String POPUP_ID_FIRST_DEPOSIT_2 = "firstDeposit2";
    private static InstructionPopupManager instance = null;
    private ArrayList<InstructionPopup> popups = new ArrayList<>();

    private InstructionPopupManager() {
        ;
    }

    public static InstructionPopupManager getInstance() {

        if (instance == null) {
            instance = new InstructionPopupManager();
        }

        return instance;
    }

    public boolean isPopupOpen(InstructionPopup popup) {
        return getPopupIndex(popup) != -1;
    }

    private int getPopupIndex(InstructionPopup popup) {
        if (popups == null) {
            return -1;
        } else {
            if (popups.isEmpty()) {
                return -1;
            } else {
                for (int i = 0; i < popups.size(); i++) {
                    if (popups.get(i).mPopupID.equals(popup.mPopupID)) {
                        return i;
                    }
                }
                return -1;
            }
        }
    }

    public void addPopup(InstructionPopup popup) {
        if (!isPopupOpen(popup)) {
            popups.add(popup);
        }
    }

    public void removePopup(InstructionPopup popup) {
        int index = getPopupIndex(popup);
        if (index != -1) {
            popups.get(index).dismiss();
            popups.remove(index);
        }
    }

    public void closeAllPopups(){
        for (int i = 0; i< popups.size(); i++){
            popups.get(0).dismiss();
            popups.remove(0);
        }
    }
}
