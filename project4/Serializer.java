import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
public class Serializer {

    public static byte[] serialize(Object obj) {
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
        catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

    public static Object deserialize(byte[] bytes) {
        try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
            try(ObjectInputStream o = new ObjectInputStream(b)){
                return o.readObject();
            }
            catch (Exception e) {
                // TODO: handle exception
            }
        }
        catch (Exception e) {
            // TODO: handle exception
        }
        return null;
    }

}
