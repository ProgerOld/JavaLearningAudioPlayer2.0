package com.example.javalearningaudioplayer20;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity  implements Runnable{
    // создание полей
    private MediaPlayer mediaPlayer = new MediaPlayer(); // создание поля медиа-плеера
    private SeekBar seekBar; // создание поля SeekBar
    private SeekBar seekBarVolume; // создание поля SeekBar
    private boolean wasPlaying = false; // поле проигрывания аудио-файла
    private boolean isChecked = false; // поле проигрывания аудио-файла
    private FloatingActionButton fabPlayPause; // поле кнопки проигрывания и постановки на паузу аудиофайла
    private FloatingActionButton fabRepeat; // поле кнопки repeart
    private FloatingActionButton fabBack; // поле кнопки back
    private FloatingActionButton fabForward; // поле кнопки forward
    private FloatingActionButton fabNext; // поле кнопки next
    private TextView seekBarHint; // поле информации у SeekBar
    private String metaData;
    private TextView metaDataAudio;

    private AudioManager audioManager; // создание поля аудио-менеджера

    private ArrayList filesName = new ArrayList();//создаем списов фалов музыки
    private int idAsset = 0; //id проигрываемого файла
    private int assetIdMax = 0; //максимальный индекс файла в списке

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findFilesMusic(); //Найти максимальное количество файлов в папке

        // получение доступа к аудио-менеджеру
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // присваивание полям id ресурсов
        fabRepeat = findViewById(R.id.fabRepeat);
        fabBack = findViewById(R.id.fabBack);
        fabPlayPause = findViewById(R.id.fabPlayPause);
        fabForward = findViewById(R.id.fabForward);
        fabNext = findViewById(R.id.fabNext);

        seekBarHint = findViewById(R.id.seekBarHint); //поле время
        seekBar = findViewById(R.id.seekBar); //поле бегунок
        seekBarVolume = findViewById(R.id.seekBarVolume); //поле бегунок
        metaDataAudio = findViewById(R.id.metaDataAudio);//поле DataAudio

        //Установка иконки по умолчанию для повтора
        fabRepeat.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.repeat_off));

        // создание слушателя изменения SeekBar
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarVolume.setOnSeekBarChangeListener(seekBarChangeListenerVolume);

        // создание слушателя нажатия кнопки
        fabPlayPause.setOnClickListener(listener);
        fabRepeat.setOnClickListener(listener);
        fabBack.setOnClickListener(listener);
        fabForward.setOnClickListener(listener);
        fabNext.setOnClickListener(listener);


        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); //максимальное значение громкости
        seekBarVolume.setMax(maxVolume); //Устанавливаем максимальное значение бегунка громкости
        //int progressVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); //Получаем текющее значение громкости
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume/2, 0);
        seekBarVolume.setProgress(maxVolume/2); //Устанавливаем текущее значение

    } //onCreate()

    //Находит максимальное количество треков
    private void findFilesMusic() {
        AssetManager myAssetManager = getApplicationContext().getAssets();
        try {
            String[] Files = myAssetManager.list(""); // массив имён файлов в папке Assets
            if (Files != null){
                for (int i = 0; i < Files.length; ++i) {
                    String fileName = Files[i];
                    if (fileName.equals("images") || fileName.equals("webkit")) {
                        continue;
                    }
                    assetIdMax = i;
                    filesName.add(Files[i]); //добаляем имя файла в список
                    Log.v("Assets:",  Files[i]); //Выводим в лог
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    } //findAssetMax()

    // создание слушателя изменения SeekBar
    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        // метод при перетаскивании ползунка по шкале,
        // где progress позволяет получить нове значение ползунка (позже progress назрачается длина трека в миллисекундах)
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int currentPosition = mediaPlayer.getCurrentPosition();
            seekBarHint.setVisibility(View.VISIBLE); // установление видимости seekBarHint
            int timeTrack = (int) Math.ceil(progress/1000f); // перевод времени из миллисекунд в секунды

            // вывод на экран времени отсчёта трека
            // Более удобный вывод seekBarHint
            LocalTime timeOfDay = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("mm:ss");
                timeOfDay = LocalTime.parse(LocalTime.ofSecondOfDay(timeTrack).format(dtf));
                seekBarHint.setText(timeOfDay.toString());
            }

            // передвижение времени отсчёта трека
            double percentTrack = progress / (double) seekBar.getMax(); // получение процента проигранного трека (проигранное время делится на длину трека)
            // seekBar.getX() - начало seekBar по оси Х
            // seekBar.getWidth() - ширина контейнера seekBar
            // 0.92 - поправочный коэффициент (так как seekBar занимает не всю ширину своего контейнера)
            seekBarHint.setX(seekBar.getX() + Math.round(seekBar.getWidth()*percentTrack*0.92));

            if (progress > 0 && mediaPlayer != null && !mediaPlayer.isPlaying()) { // если mediaPlayer не пустой и mediaPlayer не воспроизводится
//                    MainActivity.this.seekBar.setProgress(0); // установление seekBar значения 0
                seekBar.setProgress(currentPosition);
            }
        }//onProgressChanged()

        // метод при начале перетаскивания ползунка по шкале
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            seekBarHint.setVisibility(View.INVISIBLE); // установление видимости seekBarHint
        }

        // метод при завершении перетаскивания ползунка по шкале
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) { // если mediaPlayer не пустой и mediaPlayer воспроизводится
                mediaPlayer.seekTo(seekBar.getProgress()); // обновление позиции трека при изменении seekBar
            }
        }
    }; //seekBarChangeListener

    // создание слушателя изменения SeekBarVolume
    private SeekBar.OnSeekBarChangeListener seekBarChangeListenerVolume = new SeekBar.OnSeekBarChangeListener() {
        // метод при перетаскивании ползунка по шкале,
        // где progress позволяет получить нове значение ползунка (позже progress назрачается длина трека в миллисекундах)
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);  //Установка громкости
        }//onProgressChanged()

        // метод при начале перетаскивания ползунка по шкале
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        // метод при завершении перетаскивания ползунка по шкале
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }; //seekBarChangeListenerVolume


    // создание слушателя нажатия кнопок
    private  View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int idView = view.getId();
            switch (idView) {
                case R.id.fabPlayPause:
                    playSong(idAsset); // воспроизведение музыки
                    break;
                case R.id.fabRepeat:
                    // создание слушателя переключателя повтора
                    if (mediaPlayer != null){
                        isChecked = !isChecked;
                        mediaPlayer.setLooping(isChecked);
                        if (!isChecked){
                            fabRepeat.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.repeat_off));
                        }
                        else {
                            fabRepeat.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.repeat));
                        }
                    }
                    break;
                case R.id.fabBack:
                    if (mediaPlayer != null)
                        mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 5000); // переход к определённой позиции трека
                    break;
                case R.id.fabForward:
                    if (mediaPlayer != null)
                        mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 5000); // переход к определённой позиции трека
                    break;
                case R.id.fabNext:
                    clearMediaPlayer();
                    idAsset++;
                    if (idAsset <= assetIdMax){
                        playSong(idAsset); // воспроизведение музыки
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Файлов музыки больше нет!", Toast.LENGTH_LONG).show();
                    }
                    break;
            }

        }
    };


    // метод запуска аудио-файла
    public void playSong(int idAsset) {

        try {

            if (!wasPlaying || mediaPlayer == null) {
                //Запускаем новый трек

                mediaPlayer = new MediaPlayer();

                //Назначение кнопке картинку паузы
                fabPlayPause.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_pause));

                //AssetFileDescriptor descriptor = getAssets().openFd("Manowar - Manowar.mp3"); //получение файла
                AssetFileDescriptor descriptor = getAssets().openFd((String) filesName.get(idAsset)); //получение файла из списка файлов

                //Установка рессурса в плейер
                mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());

                //Получение метаданных файла
                MediaMetadataRetriever mediaMetadata = new MediaMetadataRetriever();
                mediaMetadata.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());// Загрузка файла в плейер
                metaData = mediaMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE); //Получаем название трека

                //Получаем автора файла
                String authorData = mediaMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR);
                String albumData = mediaMetadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

                if (authorData != null)  metaData += "\n" + authorData;
                if (albumData != null)  metaData += "\n" + albumData;

                descriptor.close(); //Закрытие файла
                mediaMetadata.release(); //Освобожение ресурса

                metaDataAudio.setText(metaData); //Установка текста название трека

                mediaPlayer.prepare(); //Подготовка плеера к проигрованию
                mediaPlayer.setLooping(false);
                seekBar.setMax(mediaPlayer.getDuration()); //Установка максимальной длинны трека в прогрессбар
                mediaPlayer.start(); //Старт потока
                new Thread(this).start(); //Запуск дополнительного потока

                wasPlaying = true;
            }
            else {

                //Устанавливаем паузу или возобновляем вомспроизведение
                boolean isPlay = mediaPlayer.isPlaying();

                if (mediaPlayer != null && isPlay && wasPlaying) {

                    wasPlaying = true;
                    mediaPlayer.pause();
                    //Назначение кнопке картинку плей
                    fabPlayPause.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_play));

                }
                else if(mediaPlayer != null && !isPlay && wasPlaying){
                    mediaPlayer.start();

                    new Thread(this).start(); //Запуск дополнительного потока
                    //Назначение кнопке картинку паузы
                    fabPlayPause.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_pause));

                    wasPlaying = true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    } //playSong()


    // при уничтожении активити вызов метода остановки и очиски MediaPlayer
    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearMediaPlayer();
    }

    // метод остановки и очиски MediaPlayer
    private void clearMediaPlayer() {
        mediaPlayer.stop(); // остановка медиа
        mediaPlayer.release(); // освобождение ресурсов
        mediaPlayer = null; // обнуление mediaPlayer
    }


    // метод дополнительного потока для обновления SeekBar
    @Override
    public void run() {
        int currentPosition = mediaPlayer.getCurrentPosition(); // считывание текущей позиции трека
        int total = mediaPlayer.getDuration(); // считывание длины трека

        // бесконечный цикл при условии не нулевого mediaPlayer, проигрывания трека и текущей позиции трека меньше длины трека
        while (mediaPlayer != null && mediaPlayer.isPlaying() && currentPosition < total) {
            try {

                Thread.sleep(1000); // засыпание вспомогательного потока на 1 секунду
                currentPosition = mediaPlayer.getCurrentPosition(); // обновление текущей позиции трека

            } catch (InterruptedException e) { // вызывается в случае блокировки данного потока
                e.printStackTrace();
                return; // выброс из цикла
            } catch (Exception e) {
                return;
            }

            seekBar.setProgress(currentPosition); // обновление seekBar текущей позицией трека

        }
    }

}