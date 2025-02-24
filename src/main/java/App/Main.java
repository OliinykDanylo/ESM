package App;

import Controller.Controller;

// this class was used just for testing, main application is in MainGUI class
public class Main {
    public static void main(String[] args) {

        Controller ctl = new Controller("Model1");
        ctl.readDataFrom("/Users/danylooliinyk/programming/UTP/MCS/src/main/data/data1.txt").runModel();

        String res = ctl.getResultsAsTsv();
        System.out.println(res);

        Controller ctl2 = new Controller("Model1");
        ctl2.readDataFrom("/Users/danylooliinyk/programming/UTP/MCS/src/main/data/data2.txt").runModel();

        String res2 = ctl2.getResultsAsTsv();
        System.out.println(res2);

        Controller ctl3 = new Controller("Model1");
        ctl3.readDataFrom("/Users/danylooliinyk/programming/UTP/MCS/src/main/data/data2.txt")
                .runModel()
                .runScriptFromFile("/Users/danylooliinyk/programming/UTP/MCS/src/main/scripts/script1.groovy");

        String res3 = ctl3.getResultsAsTsv();
        System.out.println(res3);

        Controller ctl4 = new Controller("Model1");
        ctl4.readDataFrom("/Users/danylooliinyk/programming/UTP/MCS/src/main/data/data1.txt")
                .runModel()
                .runScriptFromFile("/Users/danylooliinyk/programming/UTP/MCS/src/main/scripts/script2.groovy")
                .runScriptFromFile("/Users/danylooliinyk/programming/UTP/MCS/src/main/scripts/script1.groovy");

        String res4 = ctl4.getResultsAsTsv();
        System.out.println(res4);

    }
}