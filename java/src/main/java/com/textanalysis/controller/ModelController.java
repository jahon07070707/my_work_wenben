package com.textanalysis.controller;

import com.textanalysis.common.Result;
import com.textanalysis.service.ModelTrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelController {

    private final ModelTrainService modelTrainService;

    @PostMapping("/train")
    public Result<String> startTrain() {
        modelTrainService.startTrain();
        return Result.ok("训练任务已启动");
    }

    @GetMapping("/train/status")
    public Result<ModelTrainService.TrainStatus> status() {
        return Result.ok(modelTrainService.getStatus());
    }
}
