import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class Main extends Application {
	static String[] forbiddenNames = { "CON", "PRN", "AUX", "CLOCK$", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5",
			"COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9", };
	static Stage pStage;
	static Scene scene;
	static RadioButton rb1 = null;
	static Label lbl = new Label("Hello!!");
	static TextField tf3 = null;
	static TextField tf1;
	static TextField tf2;
	static GridPane pane;
	private static YouTube youtube;
	static private final String APIKEY = "AIzaSyBMKWJqJfaRnaZP9KHdSFJJmqrXsrDOe9k";


	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		try {
			pStage = primaryStage;
			MainUi gui = new MainUi();

			primaryStage.setTitle("DescriptionDumper");
			scene = new Scene(gui, 400, 140);
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class MainUi extends VBox {
		public MainUi() {
			pane = new GridPane();
			pane.setPadding(new Insets(4));
			pane.setVgap(4);
			pane.setHgap(12);

			ColumnConstraints column1 = new ColumnConstraints();
			column1.setMinWidth(64);
			ColumnConstraints column2 = new ColumnConstraints();
			column2.setHgrow(Priority.ALWAYS);
			pane.getColumnConstraints().addAll(column1, column2);

			tf1 = new TextField();
			tf2 = new TextField();
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					tf1.requestFocus();
				}
			});
			tf2.setDisable(true);
			tf1.setMaxWidth(Double.MAX_VALUE);
			tf2.setMaxWidth(Double.MAX_VALUE);

			final ToggleGroup group = new ToggleGroup();

			rb1 = new RadioButton("Channel");
			rb1.setToggleGroup(group);
			rb1.setSelected(true);
			RadioButton rb2 = new RadioButton("PlaylistId");
			rb2.setToggleGroup(group);

			group.selectedToggleProperty().addListener((ov, oldToggle, newToggle) -> {
				if (group.getSelectedToggle() == rb1) {
					tf1.setDisable(false);
					tf1.requestFocus();
					tf2.setDisable(true);
				} else {
					tf1.setDisable(true);
					tf2.setDisable(false);
					tf2.requestFocus();
				}
			});

			tf3 = new TextField();
			Button chooserButton = new Button("Select folder");
			chooserButton.setMaxWidth(Double.MAX_VALUE);
			chooserButton.setOnAction(e -> {
				DirectoryChooser chooser = new DirectoryChooser();
				chooser.setTitle("Select folder");
				File defaultDirectory = new File("c:/");
				chooser.setInitialDirectory(defaultDirectory);
				File selectedDirectory = chooser.showDialog(Main.pStage);
				tf3.setText(selectedDirectory.getAbsolutePath());
			});

			Button submitBtn = new Button("Submit");
			submitBtn.setMaxWidth(Double.MAX_VALUE);
			submitBtn.setOnAction(e -> {
				File tempFile = new File(tf3.getText());
				if (tempFile.exists() && tempFile.isDirectory() && tempFile.canWrite()) {
					List<DescriptionInfo> descriptons = null;
					if (rb1.isSelected()) {
						descriptons = getDescriptionsFromYt(tf1.getText(), true);
					} else {
						String playlistId = null;
						String info = tf2.getText();
						if (info.contains("?")) {
							String[] params = info.split("\\?");
							for (String p : params) {
								if (p.startsWith("list="))
									playlistId = p.substring(5, p.length());
							}
						}
						if (playlistId != null && !playlistId.isEmpty()) {
							lbl.setText("Getting youtube videos...");
							System.out.println("Getting youtube videos...");
							pane.setDisable(true);//Disables ui
							
							descriptons = getDescriptionsFromYt(playlistId, false);
						}
					}

						if (descriptons != null) {
							lbl.setText("Writing files....");
							System.out.println("Writing files....");
							writeTextFiles(descriptons, tempFile);
							lbl.setText("Done!");
							try {
								Runtime.getRuntime().exec("explorer.exe /select," + tempFile.getAbsolutePath());
							} catch (IOException ioe) {
								System.out.println("Can't open explorer");
							}
						} else {
							lbl.setText("Couldn't get playlist items");
						}
						pane.setDisable(false);//Enables ui
					
				}
			});

			pane.add(rb1, 0, 0);
			pane.add(rb2, 0, 1);
			pane.add(tf1, 1, 0);
			pane.add(tf2, 1, 1);
			pane.add(chooserButton, 0, 2);
			pane.add(tf3, 1, 2);
			pane.add(submitBtn, 0, 3);

			getChildren().addAll(pane, lbl);
		}

		public List<DescriptionInfo> getDescriptionsFromYt(String info, boolean isChannel) {
			try {
				List<DescriptionInfo> descriptions = new ArrayList<DescriptionInfo>();
				if (youtube == null) {
					List<String> scopes = new ArrayList<String>();
					scopes.add("https://www.googleapis.com/auth/youtube.readonly");
					GoogleCredential credential = new GoogleCredential.Builder().setTransport(new NetHttpTransport())
							.setJsonFactory(new JacksonFactory()).build();

					youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
							.setApplicationName("playa-1042").build();
				}
				String playlistId = null;
				if (isChannel) {
					YouTube.Channels.List listRequest = youtube.channels().list("contentDetails");
					listRequest.setKey(APIKEY);
					listRequest.setForUsername(info);
					listRequest.setFields("items(contentDetails)");

					ChannelListResponse response = listRequest.execute();
					List<Channel> c = new ArrayList<Channel>();

					c.addAll(response.getItems());

					for (Channel channel : c) {
						playlistId = channel.getContentDetails().getRelatedPlaylists().getUploads();
					}
				} else {
					playlistId = info;
				}

				System.out.println("Playlist Id: "+playlistId);
				if(playlistId != null){

					// Define a list to store items in the list of uploaded videos.
					List<PlaylistItem> playlistItemList = new ArrayList<PlaylistItem>();

					// Retrieve the playlist of the channel's uploaded videos.
					YouTube.PlaylistItems.List playlistItemRequest = youtube.playlistItems().list("snippet");
					playlistItemRequest.setKey(APIKEY);
					playlistItemRequest.setPlaylistId(playlistId);

					// Only retrieve data used in this application, thereby making
					// the application more efficient. See:
					// https://developers.google.com/youtube/v3/getting-started#partial
					playlistItemRequest.setFields("items(snippet/description,snippet/title),nextPageToken");
					String nextToken = "";

					// Call the API one or more times to retrieve all items in the
					// list. As long as the API response returns a nextPageToken,
					// there are still more items to retrieve.
					do {

						playlistItemRequest.setPageToken(nextToken);
						PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();
						playlistItemList.addAll(playlistItemResult.getItems());

						nextToken = playlistItemResult.getNextPageToken();
					} while (nextToken != null);

					Iterator<PlaylistItem> itemIterator = playlistItemList.iterator();

					while (itemIterator.hasNext()) {
						PlaylistItem playlistItem = itemIterator.next();

						descriptions.add(new DescriptionInfo(playlistItem.getSnippet().getTitle(),
								playlistItem.getSnippet().getDescription()));
					}
					return descriptions;
				}else{
					lbl.setText("Couldn't make playlistId");
				}
			} catch (GoogleJsonResponseException e) {
				e.printStackTrace();
				System.err.println(
						"There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());

			} catch (Throwable t) {
				t.printStackTrace();
			}
			return null;
		}

		public void writeTextFiles(List<DescriptionInfo> list, File dir) {
			PrintWriter printWriter = null;
			for (DescriptionInfo info : list) {
				if (info.getDescription().isEmpty())
					continue;
				File file = new File(dir.getAbsolutePath() + "\\" + info.getName() + ".txt");
				try {
					printWriter = new PrintWriter(file, "UTF-8");
				} catch (FileNotFoundException e) {
					System.out.println("File not found while creating playist");
				} catch (UnsupportedEncodingException e) {
					System.out.println("Unsupported Encoding in PlaylistWriter.createPlaylist");
				}

				if (printWriter != null) {
					printWriter.print(info.getDescription());
					printWriter.close();
				} else {
					System.out.println("Print writer null");
				}
			}
		}

		class DescriptionInfo {
			private String name;
			private String description;

			public DescriptionInfo(String name, String description) {
				this.name = getValidFileName(name);
				this.description = description;
			}

			public String getName() {
				return name;
			}

			public String getDescription() {
				return description;
			}
		}
	}
	//lazy way
	public static String getValidFileName(String fileName) {
		
	    String newFileName = fileName.replaceAll("^[.\\\\/:*?\"<>|]?[\\\\/:*?\"<>|]*", "");
	    for(String s : forbiddenNames) {
	    	newFileName.replace(s, "");
	    }
	    
	    if(newFileName.length()==0)
	        throw new IllegalStateException(
	                "File Name " + fileName + " results in a empty fileName!");
	    return newFileName;
	}
}
