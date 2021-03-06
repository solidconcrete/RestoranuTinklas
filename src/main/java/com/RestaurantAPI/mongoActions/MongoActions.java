package com.RestaurantAPI.mongoActions;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import org.hibernate.validator.constraints.Email;
import org.json.simple.JSONObject;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;

public class MongoActions {
    private static String uri = "mongodb+srv://admin:TrtY2c94xzxdDrj@cluster0-ekcge.mongodb.net/test?retryWrites=true&w=majority";
    private static MongoClient mongoClient = new MongoClient(new MongoClientURI(uri));

    public static MongoCollection getCollection(String collectionName)
    {
//        String uri = "mongodb+srv://admin:TrtY2c94xzxdDrj@cluster0-ekcge.mongodb.net/test?retryWrites=true&w=majority";
//        mongoClient = new MongoClient(new MongoClientURI(uri));
        MongoDatabase db = mongoClient.getDatabase("restaurant_chain");
        MongoCollection<Document> collection = db.getCollection(collectionName);
        return collection;
    }

    public static ArrayList<JSONObject> getManagedRestaurants(String Email)
    {
        ArrayList<JSONObject> addresses = new ArrayList<>();
        MongoCollection<Document> collection = MongoActions.getCollection("workers");
        Document workerDoc = collection.find(new Document("Email", Email)).first();
        String duty = (String) workerDoc.get("Duty");
        if (duty.equals("Restaurant_chain_manager"))
        {
            System.out.println("MANAGER DETECTED");
            addresses = getRestaurantsFromChainName(workerDoc.getString("Managed_restaurant_chain"));
        }
        else
        {
            System.out.println("WORKER DETECTED");
            JSONObject restaurantJson = new JSONObject();
            restaurantJson.put("id", 1);
            restaurantJson.put("address", workerDoc.getString("Managed_restaurant"));
            System.out.println(restaurantJson);
            addresses.add (restaurantJson);
        }
        return addresses;
    }

    public static ArrayList<JSONObject> getRestaurantsFromChainName(String chainName)
    {
        ArrayList<JSONObject> addresses = new ArrayList<>();
        int i = 0;
        MongoCollection restaurantsCollection = MongoActions.getCollection("restaurants");
        MongoCursor<Document> restaurants = restaurantsCollection.find().iterator();
        try {
            while (restaurants.hasNext())
            {
                Document restaurant = restaurants.next();
                if (((String) restaurant.get("Restaurant_chain")).equals(chainName))
                {
                    JSONObject restaurantJson = new JSONObject();
                    String id = restaurant.getObjectId("_id").toString();
                    restaurantJson.put("_id", id);
                    restaurantJson.put("address", (String) restaurant.get("Address"));
                    System.out.println(restaurantJson);

                    addresses.add(restaurantJson);

                    i++;
                }
            }
            restaurants.close();
        }
        finally {
            restaurants.close();
        }
        return addresses;
    }

    public static ArrayList<JSONObject> getRestaurantDishes(String restaurantAddress)
    {
        ArrayList<JSONObject> dishes= new ArrayList<>();
        MongoCollection restaurantCollection = MongoActions.getCollection("restaurants");
        MongoCollection dishCollection = MongoActions.getCollection("dishes");
        Document restaurantDoc = (Document) restaurantCollection.find(new Document("Address", restaurantAddress)).first();
        Document dishDoc;
        BasicDBObject query = new BasicDBObject();
        List<String > allDishes = (List<String>) restaurantDoc.get("Dishes");
        for (String id : allDishes)
        {
            JSONObject dish = new JSONObject();
            query.put("_id", new ObjectId(id));
            dishDoc =(Document) dishCollection.find(query).first();
            dish.put("name", (String) dishDoc.get("Dish_name"));
            dish.put("img_url", (String) dishDoc.get("Image_link"));
            dish.put("price", (Double) dishDoc.get("Price"));
            dish.put("id", id);
            dishes.add(dish);
//            dishNames.add((String) dishDoc.get("Dish_name"));
        }
        return dishes;
    }

    public static String getChainByRestaurantAddress (String restaurantAddress)
    {
        System.out.println("Got address: " + restaurantAddress);
        MongoCollection<Document> collection = MongoActions.getCollection("restaurants");
        Document restaurantDoc = collection.find(new Document("Address", restaurantAddress)).first();
        return (String) restaurantDoc.get("Restaurant_chain");
    }

    public static JSONObject getAddressOrChain(String Email)
    {
        JSONObject addressOrChain = new JSONObject();
        MongoCollection<Document> collection = MongoActions.getCollection("workers");
        Document workerDoc = collection.find(new Document("Email", Email)).first();

        String duty = (String) workerDoc.get("Duty");
        if (duty.equals("Restaurant_chain_manager"))
        {
            addressOrChain.put("chain", workerDoc.get("Managed_restaurant_chain"));
//            return (String) workerDoc.get("Managed_restaurant_chain");
        }
        else
        {
            addressOrChain.put("address", workerDoc.get("Managed_restaurant"));
//            return (String) workerDoc.get("Managed_restaurant");
        }
        return addressOrChain;
    }

    public static String getLogoByChainName(String chainName)
    {
        MongoCollection<Document> collection = MongoActions.getCollection("restaurant_chains");
        Document chainDoc = collection.find(new Document("Restaurant_chain_name", chainName)).first();
        String imgUrl = (String) chainDoc.get("Chain_logo_link");
        return imgUrl;

    }

    public static String getChainLogoByEmail(String Email)
    {
        MongoCollection<Document> collection = MongoActions.getCollection("workers");
        Document workerDoc = collection.find(new Document("Email", Email)).first();
        String duty = (String) workerDoc.get("Duty");
        if (duty.equals("Restaurant_chain_manager"))
        {
            System.out.println("MANAGER DETECTED");
            return getLogoByChainName(workerDoc.getString("Managed_restaurant_chain"));
        }
        else
        {
            String chainName = getChainByRestaurantAddress(workerDoc.getString("Managed_restaurant"));
            System.out.println("Chain name : " + chainName);
            return getLogoByChainName(chainName);
        }
    }

    public static String getWorkerDuty(String Email)
    {
        MongoCollection<Document> collection = MongoActions.getCollection("workers");
        Document workerDoc = collection.find(new Document("Email", Email)).first();
        String duty = (String) workerDoc.get("Duty");
        return duty;
    }

    public static ArrayList<JSONObject> getRestaurantAdmin(String restaurantAddress)
    {
        ArrayList<JSONObject> workersData = new ArrayList<>();
        MongoCollection<Document> collection = MongoActions.getCollection("workers");
        MongoCursor<Document> workers = collection.find(new Document("Managed_restaurant", restaurantAddress)).iterator();
        while (workers.hasNext())
        {
            JSONObject workerData = new JSONObject();
            Document worker = workers.next();
            workerData.put("id", worker.getObjectId("_id").toString());
            workerData.put("name", worker.getString("Name"));
            workerData.put("surname", worker.getString("Surname"));
            workerData.put("email", worker.getString("Email"));
            workersData.add(workerData);
        }
        return workersData;
    }

    public static ArrayList<JSONObject> getChainDishes(String chainName)
    {
        ArrayList<JSONObject> chainDishes = new ArrayList<>();
        MongoCollection dishesCollection = MongoActions.getCollection("dishes");;
        MongoCursor<Document> dishDedails = dishesCollection.find(new Document("Restaurant_chain", chainName)).iterator();

            while (dishDedails.hasNext())
            {
                Document tempDish = dishDedails.next();
                JSONObject dish = new JSONObject();

                dish.put("name", (String) tempDish.get("Dish_name"));
                dish.put("img_url", (String) tempDish.get("Image_link"));
                dish.put("price", (Double) tempDish.get("Price"));
                dish.put("id", tempDish.getObjectId("_id").toString());
                chainDishes.add(dish);
            }
            return chainDishes;
    }


    public static String getManagedRestaurantChain(String Email)
    {
        MongoCollection<Document> collection = MongoActions.getCollection("workers");
        Document workerDoc = collection.find(new Document("Email", Email)).first();
        String chainName = (String) workerDoc.get("Managed_restaurant_chain");
        return chainName;
    }

    public static JSONObject getWorkerData(String Email)
    {
        MongoCollection<Document> collection = MongoActions.getCollection("workers");
        Document workerDoc = collection.find(new Document("Email", Email)).first();
        JSONObject workerData = new JSONObject();
        workerData.put("id",workerDoc.getObjectId("_id").toString());
        workerData.put("name", workerDoc.getString("Name"));
        workerData.put("surname", workerDoc.getString("Surname"));
        workerData.put("email", workerDoc.getString("Email"));
        return workerData;
    }

    public static Boolean changeDishPrice (String dishName, String chainName, double newPrice)
    {
        MongoCollection<Document> dishCollection = MongoActions.getCollection("dishes");


        Bson filter = and(eq("Dish_name", dishName), eq("Restaurant_chain", chainName));


        Bson updateOperation = set("Price", newPrice);
        UpdateResult result = dishCollection.updateOne(filter, updateOperation);
        System.out.println(result);
        if (result.getModifiedCount() != 1)
        {
            return false;
        }
        return true;
    }

    public static String getChainFromWorkerEmail (String Email)
    {
        MongoCollection<Document> workerCollection = MongoActions.getCollection("workers");
        Document workerDoc = workerCollection.find(new Document("Email", Email)).first();
        String restaurantAddress = workerDoc.getString("Managed_restaurant");

        MongoCollection<Document> restaurantCollection = MongoActions.getCollection("restaurants");
        Document restaurantDoc = restaurantCollection.find(new Document("Address", restaurantAddress)).first();
        String chain = restaurantDoc.getString("Restaurant_chain");
        return chain;
    }

    public static void addDish (String dishName, String dishPrice, String imgUrl, String [] dishIngredients, String chainName)
    {
        MongoCollection<Document> dishCollection = MongoActions.getCollection("dishes");
        ArrayList<String> ingredients = new ArrayList<>();
        for (int i = 0; i < dishIngredients.length; i++)
        {
            ingredients.add(dishIngredients[i]);
        }
        Document dishDoc = new Document("Dish_name", dishName).append("Restaurant_chain", chainName)
                .append("Image_link", imgUrl).append("Price", dishPrice)
                .append("Ingredients", ingredients);
        dishCollection.insertOne(dishDoc);
    }

    public static String getChainFromManagerEmail (String Email)
    {
        MongoCollection<Document> workerCollection = MongoActions.getCollection("workers");
        Document workerDoc = workerCollection.find(new Document("Email", Email)).first();
        String chainName = workerDoc.getString("Managed_restaurant_chain");
        return chainName;
    }

    public static String getAddressFromWorkerEmail (String Email)
    {
        MongoCollection<Document> workerCollection = MongoActions.getCollection("workers");
        Document workerDoc = workerCollection.find(new Document("Email", Email)).first();
        String restaurantAddress = workerDoc.getString("Managed_restaurant");
        return restaurantAddress;
    }

    public static String getChainLogoByChainName (String chainName)
    {
        MongoCollection<Document> workerCollection = MongoActions.getCollection("restaurant_chains");
        Document workerDoc = workerCollection.find(new Document("Restaurant_chain_name", chainName)).first();
        return workerDoc.getString("Chain_logo_link");
    }

    public static String getUserPassword (String Email)
    {
        MongoCollection<Document> workerCollection = MongoActions.getCollection("workers");
        Document workerDoc = workerCollection.find(new Document("Email", Email)).first();
        String password = workerDoc.getString("Password");
        return password;
    }

    public static boolean changeUserPassword (String Email, String newPassword)
    {
        MongoCollection<Document> workerCollection = MongoActions.getCollection("workers");
        Bson filter = eq("Email", Email);
        Bson updateOperation = set("Password", newPassword);
        UpdateResult result = workerCollection.updateOne(filter, updateOperation);
        if (result.getModifiedCount()  != 0)
        {
            return true;
        }
        return false;
    }

    public static void addRestaurant (String chainName, String restaurantAddress)
    {
        MongoCollection<Document> restaurantCollection = MongoActions.getCollection("restaurants");
        Document restaurantDoc = new Document("Address", restaurantAddress)
                .append("Restaurant_chain", chainName);
        restaurantCollection.insertOne(restaurantDoc);
    }
}
