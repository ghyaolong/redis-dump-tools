import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * 用于导出redis数据
 * User: zhaodp
 * Updater:tututu
 * Date: 2016-7-15
 * UpdateDate:2019-09-09
 */
public class RedisDumper {
    @Parameter(names = {"--host", "-h"}, description = "redis server host")
    String host;
    @Parameter(names = {"--port", "-p"}, description = "redis server host")
    int port = 6379;
    @Parameter(names = {"--password", "-a"}, description = "redis auth")
    String password;
    /*@Parameter(names = {"--index", "-i"}, description = "redis db index")
    int index;*/
    /*@Parameter(names = {"--dataPath", "-d"}, description = "the path where dumped data save to ")
    String dataPath;*/

    @Parameter(names={"--folder","-f"},description = "folderName")
    String folder;

    public static void main(String[] args) {
        RedisDumper redisDumper = new RedisDumper();
        /*String[] argvs = {"-h","127.0.0.1","-p","6379","-f","E:/redisBack/20190909173000"};*/
        new JCommander(redisDumper, args);
        redisDumper.doDump();
    }

    private void doDump() {
        Jedis jedis = new Jedis(host,port);
        if(password != null)
          jedis.auth(password);

        for (int index = 0; index < 16; index++) {
            jedis.select(index);
            BufferedWriter bw = null;
            try {

                File file = new File(folder);
                if(!file.exists()&& !file.isDirectory()){
                    file.mkdirs();
                }

                File dataFile = new File(folder+File.separator+"db["+index+"].txt");
                FileWriter fileWriter = new FileWriter(dataFile);
                bw = new BufferedWriter(fileWriter);

                ScanResult<String> result = jedis.scan("0");
                while (true) {
                    List<String> keyList = result.getResult();
                    for (int i = 0; i < keyList.size(); i++) {
                        String key = keyList.get(i);
                        if ("string".equals(jedis.type(key))) {
                            bw.write(StringKeyValue.getJsonLine(jedis, key));
                            bw.newLine();
                        } else if ("zset".equals(jedis.type(key))) {
                            bw.write(ZSetKeyValue.getJsonLine(jedis, key));
                            bw.newLine();
                        } else if ("set".equals(jedis.type(key))) {
                            bw.write(SetKeyValue.getJsonLine(jedis, key));
                            bw.newLine();
                        } else  if("list".equals(jedis.type(key))) {
                            bw.write(ListKeyValue.getJsonLine(jedis, key));
                            bw.newLine();
                        } else  if("hash".equals(jedis.type(key))) {
                            bw.write(HashKeyValue.getJsonLine(jedis, key));
                            bw.newLine();
                        }
                    }
                    //遍历结束
                    if("0".equals(result.getStringCursor())){
                        break;
                    }
                    result = jedis.scan(result.getStringCursor());
                    System.out.println("redis db["+index+"] back Complete!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
                if(bw != null){
                    try {
                        bw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println("redis all db back Complete!");
    }
}
