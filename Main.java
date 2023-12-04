import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
class Admin
{
    int ID;
    String pass;
    ChargingStation cs;
   public Admin(int ID,String pass, ChargingStation cs)
   {
    this.ID=ID;
    this.pass=pass;
    this.cs=cs;
   }

}
class TC extends Thread
{
    ChargingStation chargingstation;
    Car c;
    FileWriter f;
    public TC(Car c,ChargingStation chargingstation,FileWriter f)
    {
        this.chargingstation=chargingstation;
        this.c=c;
        this.f=f;
    }

    public synchronized void run()
    {
        for(int i=1;i<=3;i++)
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //System.out.println(i+" Second over for "+c.ID());
        }
        if(chargingstation.waitingList.contains(c))
        {
            //System.out.println(c.ID()+" removing inside time checker....");
            System.out.println(c.ID()+" waited for so long in Waiting List, hence it is leaving without charging.");
            try {
                f.write(c.ID()+" waited for so long in Waiting List, hence it is leaving without charging.");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            chargingstation.waitingList.remove(c);
            System.out.println(c.ID()+ "Left without charging");
            try {
                f.write(c.ID()+ "Left without charging");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //System.out.println(c.ID()+" removed inside time checker....");
            //System.out.println("gonna stop "+c.ID()+" thread.");
            Thread.interrupted();
        }
        else{
            Thread.interrupted();
            //System.out.println("stopped "+c.ID()+"TC thread.");
        }
    }
}

class ChargingStation {
    int id;
    int location;
    int totalSlots;
    int availableSlots;
    public List<Car> waitingList;
    public FileWriter f;
    Car c;

    public ChargingStation(int id, int location, int totalSlots, FileWriter f) {
        this.id = id;
        this.location = location;
        this.totalSlots = totalSlots;
        this.availableSlots = totalSlots;
        this.waitingList = new ArrayList<>();
        this.f=f;
    }

    public static String assignEnrgySourse()
    {
        Random rand = new Random();
        int esourse = rand.nextInt(3);
        String assignedString = eSourseGen(esourse);
        return assignedString;
    }

    public static String eSourseGen(int value) {
        String result = "";
        switch (value) {
            case 0:
                result = "Solar";
                break;
            case 1:
                result = "Wind";
                break;
            case 2:
                result = "Power Grid";
                break;
            default:
                result = "Invalid value";
                break;
        }
        return result;
    }
    void timechecker(Car c, FileWriter f) throws InterruptedException
    {
         TC tc=new TC(c,this,f);
         tc.start();
         //tc.join();   
    }
    public synchronized boolean bookslot(Car car, int duration) throws InterruptedException {
        c=car;
        if (availableSlots > 0) {
            System.out.println(car.ID() + " will be charging for " + duration + " Minutes in Station"+id +" ,with Energy Sourse as: "+assignEnrgySourse());
            try {
                f.write(car.ID() + " Charged for " + duration + " Minutes in Station"+id+" ,with Energy Sourse as: "+assignEnrgySourse()+"\n");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            availableSlots--;
            return true;
        } else {
            System.out.println(car.ID() + " is added to the waiting list.");
            try {
                f.write(car.ID() + " is added to the waiting list.\n");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (!waitingList.contains(car)) {
                waitingList.add(car);
                timechecker(car,f);
            }
            try {
                wait(); // Car is added to the waiting list
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false; // Room booking failed
        }
    }

    public synchronized void releaseSlot(Car car) throws InterruptedException {
        availableSlots++;
        System.out.println(car.ID()+" Charging slot released. Available slots: " + availableSlots);
         try {
                f.write(car.ID()+" Charging slot released. Available slots: " + availableSlots+"\n");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        checkWaitingList();
        notify(); // Notify waiting cars about the availability of a slot
    }

    private void checkWaitingList() throws InterruptedException{
        while (!waitingList.isEmpty() && availableSlots > 0) {
            Car car = waitingList.remove(0);
            if (bookslot(car, car.chargingDuration())) {
                System.out.println(car.ID() + " moved from the waiting list and got a slot.");
            }
        }
    }
}

class Car extends Thread {
    private String ID;
    private int location;
    private int chargingDuration;
    private ChargingStation[] stations;
    private ChargingStation nearestStation;

    public Car(String ID, int location, int chargingDuration, ChargingStation[] stations) {
        this.ID = ID;
        this.location = location;
        this.chargingDuration = chargingDuration;
        this.stations = stations;
        this.nearestStation = findNearestStation();
    }

    public ChargingStation findNearestStation() {
        int minDistance = Integer.MAX_VALUE;
        ChargingStation nearest = null;

        for (ChargingStation station : stations) {
            int distance = Math.abs(station.location - this.location);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = station;
            }
        }
        //System.out.println("Nearest Station for " + this.ID() + " is at location " + nearest.location);
        return nearest;
    }

    public String ID() {
        return ID;
    }

    public int chargingDuration() {
        return chargingDuration;
    }

    public void run() {
        try {
            if (nearestStation.bookslot(this, chargingDuration)) {
                try {
                    Thread.sleep(chargingDuration * 1000); // Simulating charging duration in seconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                nearestStation.releaseSlot(this);
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        String currentDirectory = System.getProperty("user.dir");
        String newDirectoryName = "StationLogFiles";
        File newDirectory = new File(currentDirectory, newDirectoryName);
        
        if (!newDirectory.exists()) {
            if (newDirectory.mkdir()) {
                System.out.println("Directory created: " + newDirectory.getAbsolutePath());
            } else {
                System.out.println("Failed to create directory.");
                return;
            }
        }
        File s1file=new File(newDirectory,"Station1LogFiles");
        File s2file=new File(newDirectory,"Station2LogFiles");
        try {
            if(s1file.createNewFile()){
                System.out.println("Station 1 Log files created");
            }
            
        } catch (IOException e) {
            System.out.println("Error Occured in creating Station 1 Log files");
            e.printStackTrace();
        }
        try {
            if(s2file.createNewFile()){
                System.out.println("Station 2 Log files created");
            }
            
        } catch (IOException e) {
            System.out.println("Error Occured in creating Station 2 Log files");
            e.printStackTrace();
        }
        FileWriter f1=new FileWriter(s1file.getAbsolutePath());
        FileWriter f2 = new FileWriter(s2file.getAbsolutePath());
        System.out.println(s1file.getAbsolutePath());
        

        ChargingStation station1 = new ChargingStation(1, 1, 2,f1); // Create charging stations
        ChargingStation station2 = new ChargingStation(2, 7, 2,f2);

        Admin as1=new Admin(001,"admin1",station1);
        Admin as2=new Admin(002,"admin2",station2);
    

        ChargingStation[] stations = {station1, station2}; // Create an array of charging stations

        // Create cars with different charging durations and locations
        Car car1 = new Car("car1", 1, 2, stations);
        Car car2 = new Car("car2", 2, 3, stations);
        Car car3 = new Car("car3", 3, 1, stations);
        Car car4 = new Car("car4", 4, 3, stations);
        Car car5 = new Car("car5", 5,3, stations);
        Car car6 = new Car("car6",6, 2, stations);
        Car car7 = new Car("car7",7, 4, stations);
        Car car8 = new Car("car8",8, 1, stations);

        // Start threads for each car
        car1.start();
        car2.start();
        car3.start();
        car4.start();
        car5.start();
        car6.start();
        car7.start();
        car8.start();

        // while (car1.isAlive() || car2.isAlive() || car3.isAlive() || car4.isAlive()||car5.isAlive() || car6.isAlive() || car7.isAlive() || car8.isAlive()) {
        //     try {
        //         Thread.sleep(1000); // Sleep for a short duration before rechecking
        //     } catch (InterruptedException e) {
        //         e.printStackTrace();
        //     }
        // }
        car1.join();
        car2.join();
        car3.join();
        car4.join();
        car5.join();
        car6.join();
        car7.join();
        car8.join();
      
        // f1.flush();
        // f2.flush();
        f1.close();
        f2.close();
        Thread.sleep(10000);
        System.out.println("All cars have finished their charging. Program terminated.");
        System.out.println("=========================================================================================================");

        //System.out.println("All cars have finished their charging. Program terminated.");
        Scanner sc=new Scanner(System.in);
        System.out.print("Would You like to View log files(yes/no)");
        if("yes".equals(sc.next()))
        {
            System.out.print("Would you like to view Station log files(yes/no)");
             if("yes".equals(sc.next()))
             {
                System.out.print("Station1 or Station2:");
                String sname=sc.next();
                if("Station1".equals(sname))
                {
                    System.out.print("Enter your ID:");
                    int id=sc.nextInt();
                    System.out.print("Enter your password:");
                    String pass=sc.next();
                    if(id==as1.ID && pass.equals(as1.pass))
                    {
                        System.out.println("*******************************************************************");
                        System.out.println("File Name: "+s1file.getName()+"\n"+"File Length: "+s1file.length()+"\n"+"File Last Modified: "+s1file.lastModified()+"\n");
                        Scanner myreader=new Scanner(s1file);
                        while(myreader.hasNext())
                        {
                            System.out.println(myreader.nextLine());
                        }
                        myreader.close();
                        System.out.println("*******************************************************************");
                        System.out.println("Would u like to delete the file(yes/no):");
                        if("yes".equals(sc.next()))
                        {
                            s1file.delete();
                        }
                        else{
                            System.out.println("Thank u then.----End of Program----");
                        }
                        
                    }
                    else{
                        System.out.println("Wrong credentials, Get Lost.----End of Program----");
                    }
                }
                else if("Station1".equals(sname))
                {
                    System.out.print("Enter your ID:");
                    int id=sc.nextInt();
                    System.out.print("Enter your password:");
                    String pass=sc.next();
                    if(id==as2.ID && pass.equals(as2.pass))
                    {
                        System.out.println("*******************************************************************");
                        System.out.println("File Name: "+s2file.getName()+"\n"+"File Length: "+s2file.length()+"\n"+"File Last Modified: "+s2file.lastModified()+"\n");
                        Scanner myreader=new Scanner(s2file);
                        while(myreader.hasNext())
                        {
                            System.out.println(myreader.nextLine());
                        }
                        System.out.println("*******************************************************************");
                        myreader.close();
                        System.out.println("Would u like to delete the file(yes/no):");
                        if("yes".equals(sc.next()))
                        {
                            s2file.delete();
                        }
                        else{
                            System.out.println("Thank u then.----End of Program----");
                        }
                        
                    }
                    else{
                        System.out.println("Wrong credentials, Get Lost.----End of Program----");
                    }
                }
                else{
                    System.out.println("Nothing exists like that. ----End of Program----");
                }
             }
              else{
                System.out.println("Then y did u enter \"yes\" before, Don't waste my time. ----End of Program----");
        }
        }
        else{
            System.out.println("Then thank you. ----End of Program----");
        }
        
    }
}